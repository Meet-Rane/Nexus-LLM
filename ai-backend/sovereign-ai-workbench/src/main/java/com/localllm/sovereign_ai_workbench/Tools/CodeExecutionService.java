package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Dto.ArtifactDto;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class CodeExecutionService {

    @Value("${sandbox.docker.command:docker}")
    private String dockerCommand;

    private final ArtifactStorageService artifactStorageService;
    private final ArtifactService artifactService;

    // Fast 30-second sandbox execution timeout
    private static final int TIMEOUT_SECONDS = 30;

    public CodeExecutionService(
            ArtifactStorageService artifactStorageService,
            ArtifactService artifactService) {
        this.artifactStorageService = artifactStorageService;
        this.artifactService = artifactService;
    }

    public CodeExecutionResult execute(CodeExecutionRequest request) {
        Path workspace = null;
        Process process = null;

        String executionId = UUID.randomUUID().toString();
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "sandbox-" + executionId;
        }

        try {
            // 1. Create unique sandbox workspace
            workspace = Files.createTempDirectory("sandbox-" + executionId + "-");
            Path outputDirectory = workspace.resolve("output");
            Files.createDirectories(outputDirectory);

            // 2. If conversation folder exists, copy its existing files into sandbox
            final Path sandboxWs = workspace;
            try {
                Path convDir = artifactStorageService.getConversationDirectory(conversationId);
                if (Files.exists(convDir)) {
                    try (Stream<Path> stream = Files.walk(convDir)) {
                        stream.filter(Files::isRegularFile).forEach(source -> {
                            try {
                                Path rel = convDir.relativize(source);
                                Path target = sandboxWs.resolve(rel).normalize();
                                if (target.startsWith(sandboxWs)) {
                                    if (target.getParent() != null) {
                                        Files.createDirectories(target.getParent());
                                    }
                                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (Exception ignored) {
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            }

            // 3. Write LLM-generated files in request payload over workspace files
            if (request.getFiles() != null) {
                for (var entry : request.getFiles().entrySet()) {
                    Path file = workspace.resolve(entry.getKey()).normalize();

                    if (!file.startsWith(workspace)) {
                        throw new IllegalArgumentException("Invalid file path: " + entry.getKey());
                    }

                    if (file.getParent() != null) {
                        Files.createDirectories(file.getParent());
                    }

                    Files.writeString(file, entry.getValue());
                }
            }

            // 4. Start Docker container
            ProcessBuilder processBuilder = new ProcessBuilder(
                    dockerCommand,
                    "run",
                    "--rm",
                    "--network", "none",
                    "--memory", "256m",
                    "--cpus", "1",
                    "--pids-limit", "100",
                    "--read-only",
                    "--tmpfs", "/tmp",
                    "-v", workspace.toAbsolutePath() + ":/sandbox:rw",
                    "sovereign-python-sandbox",
                    "python",
                    "/sandbox/" + request.getEntryFile()
            );

            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // Immediately close stdin so interactive input() calls don't hang execution
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {
            }

            // 5. Wait for execution completion (max 30 seconds)
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return new CodeExecutionResult(
                        -1,
                        "Execution timed out after " + TIMEOUT_SECONDS + " seconds (Check for infinite loops or interactive input).",
                        true,
                        List.of()
                );
            }

            // 6. Read stdout/stderr
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 7. Store generated output files permanently in ArtifactService
            List<String> artifactNames = new ArrayList<>();
            if (Files.exists(outputDirectory)) {
                final String activeConvId = conversationId;
                try (Stream<Path> stream = Files.walk(outputDirectory)) {
                    stream.filter(Files::isRegularFile).forEach(file -> {
                        try {
                            Path relativePath = outputDirectory.relativize(file);
                            String relPathStr = "output/" + relativePath.toString().replace('\\', '/');
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            Artifact artifact = artifactService.saveFile(activeConvId, relPathStr, content);
                            artifactNames.add(artifact.getFilePath());
                            
                            // Emit artifact created event for UI rendering
                            ConversationContextHolder.emitEvent(AgentStreamEvent.artifactCreated(ArtifactDto.fromEntity(artifact)));
                        } catch (Exception e) {
                            // Fallback to storing binary/raw bytes
                            try {
                                Path relativePath = outputDirectory.relativize(file);
                                String relPathStr = "output/" + relativePath.toString().replace('\\', '/');
                                byte[] bytes = Files.readAllBytes(file);
                                Artifact artifact = artifactService.saveFileBytes(activeConvId, relPathStr, bytes);
                                artifactNames.add(relPathStr);
                                if (artifact != null) {
                                    ConversationContextHolder.emitEvent(AgentStreamEvent.artifactCreated(ArtifactDto.fromEntity(artifact)));
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    });
                }
            }

            // 8. Return result
            return new CodeExecutionResult(
                    process.exitValue(),
                    output.toString(),
                    false,
                    artifactNames
            );

        } catch (Exception e) {
            return new CodeExecutionResult(
                    -1,
                    "Sandbox execution failed: " + e.getMessage(),
                    false,
                    List.of()
                );
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            if (workspace != null) {
                deleteWorkspace(workspace);
            }
        }
    }

    private void deleteWorkspace(Path workspace) {
        try (Stream<Path> stream = Files.walk(workspace)) {
            stream.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}