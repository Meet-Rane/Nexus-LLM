package com.localllm.sovereign_ai_workbench.Tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    @Value("${sandbox.docker.command:docker}")
    private String dockerCommand;

    private final ArtifactStorageService artifactStorageService;

    private static final int TIMEOUT_SECONDS = 1000;

    public CodeExecutionService(
            ArtifactStorageService artifactStorageService) {

        this.artifactStorageService = artifactStorageService;
    }

    public CodeExecutionResult execute(CodeExecutionRequest request) {

        Path workspace = null;
        Process process = null;

        String executionId = UUID.randomUUID().toString();

        try {

            // 1. Create unique workspace
            workspace = Files.createTempDirectory(
                    "sandbox-" + executionId + "-"
            );

            Path outputDirectory =
                    workspace.resolve("output");

            Files.createDirectories(outputDirectory);

            // 2. Write LLM-generated files
            for (var entry : request.getFiles().entrySet()) {

                Path file = workspace
                        .resolve(entry.getKey())
                        .normalize();

                // Prevent ../ path traversal
                if (!file.startsWith(workspace)) {
                    throw new IllegalArgumentException(
                            "Invalid file path: " + entry.getKey()
                    );
                }

                if (file.getParent() != null) {
                    Files.createDirectories(file.getParent());
                }

                Files.writeString(
                        file,
                        entry.getValue()
                );
            }

            // 3. Start Docker
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

                    "-v",
                    workspace.toAbsolutePath()
                            + ":/sandbox:rw",

                    "sovereign-python-sandbox",

                    "python",
                    "/sandbox/" + request.getEntryFile()
            );

            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();

            // 4. Wait for execution
            boolean finished = process.waitFor(
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!finished) {

                process.destroyForcibly();

                return new CodeExecutionResult(
                        -1,
                        "Execution timed out after "
                                + TIMEOUT_SECONDS
                                + " seconds.",
                        true,
                        List.of()
                );
            }

            // 5. Read stdout/stderr
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {

                    output.append(line)
                            .append("\n");
                }
            }

            // 6. Store generated files permanently
            List<StoredArtifact> storedArtifacts =
                    artifactStorageService.storeArtifacts(
                            outputDirectory,
                            executionId
                    );

            // 7. Convert stored artifacts to names
            List<String> artifactNames =
                    storedArtifacts.stream()
                            .map(StoredArtifact::name)
                            .toList();

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
                    "Sandbox execution failed: "
                            + e.getMessage(),
                    false,
                    List.of()
            );

        } finally {

            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            // Delete temporary workspace
            if (workspace != null) {
                deleteWorkspace(workspace);
            }
        }
    }

    private void deleteWorkspace(Path workspace) {

        try {

            Files.walk(workspace)
                    .sorted((a, b) -> b.compareTo(a))
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