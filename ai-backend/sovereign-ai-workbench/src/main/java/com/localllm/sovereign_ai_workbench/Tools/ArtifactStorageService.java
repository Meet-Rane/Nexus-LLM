package com.localllm.sovereign_ai_workbench.Tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ArtifactStorageService {

    private final Path storageRoot;

    public ArtifactStorageService(
            @Value("${sandbox.artifact-storage:storage/artifacts}")
            String storagePath) {

        this.storageRoot = Path.of(storagePath)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create artifact storage directory",
                    e
            );
        }
    }

    public Path getConversationDirectory(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be empty");
        }
        Path convDir = storageRoot.resolve(conversationId).normalize();
        if (!convDir.startsWith(storageRoot)) {
            throw new SecurityException("Invalid conversation ID path traversal attempt");
        }
        try {
            Files.createDirectories(convDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create conversation artifact directory", e);
        }
        return convDir;
    }

    public Path resolveConversationPath(String conversationId, String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        // Clean relative path (remove leading slashes/backslashes)
        String cleanPath = relativePath.replaceAll("^[\\\\/]+", "");
        Path convDir = getConversationDirectory(conversationId);
        Path resolvedPath = convDir.resolve(cleanPath).normalize();

        if (!resolvedPath.startsWith(convDir)) {
            throw new SecurityException("Security violation: Path traversal attempt detected ('" + relativePath + "')");
        }

        return resolvedPath;
    }

    public Path saveFileContent(String conversationId, String relativePath, String content) {
        byte[] bytes = (content != null) ? content.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return saveFileBytes(conversationId, relativePath, bytes);
    }

    public Path saveFileBytes(String conversationId, String relativePath, byte[] bytes) {
        Path targetPath = resolveConversationPath(conversationId, relativePath);
        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file artifact at: " + relativePath, e);
        }
    }

    public String readFileContent(String conversationId, String relativePath) {
        Path targetPath = resolveConversationPath(conversationId, relativePath);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new IllegalArgumentException("File not found: " + relativePath);
        }
        try {
            return Files.readString(targetPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file artifact: " + relativePath, e);
        }
    }

    public byte[] readFileBytes(String conversationId, String relativePath) {
        Path targetPath = resolveConversationPath(conversationId, relativePath);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new IllegalArgumentException("File not found: " + relativePath);
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes: " + relativePath, e);
        }
    }

    public List<String> listRelativePaths(String conversationId) {
        Path convDir = getConversationDirectory(conversationId);
        if (!Files.exists(convDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(convDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(convDir::relativize)
                    .map(Path::toString)
                    .map(p -> p.replace('\\', '/'))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list files in artifact directory", e);
        }
    }

    public boolean deleteFile(String conversationId, String relativePath) {
        Path targetPath = resolveConversationPath(conversationId, relativePath);
        try {
            return Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file artifact: " + relativePath, e);
        }
    }

    public List<StoredArtifact> storeArtifacts(Path outputDirectory, String executionId) {
        List<StoredArtifact> artifacts = new ArrayList<>();
        try {
            if (!Files.exists(outputDirectory)) {
                return artifacts;
            }

            Path executionDirectory = storageRoot.resolve(executionId).normalize();
            Files.createDirectories(executionDirectory);

            try (Stream<Path> stream = Files.walk(outputDirectory)) {
                stream.filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                Path relativePath = outputDirectory.relativize(file);
                                Path destination = executionDirectory.resolve(relativePath).normalize();

                                if (!destination.startsWith(executionDirectory)) {
                                    throw new SecurityException("Invalid artifact path");
                                }

                                if (destination.getParent() != null) {
                                    Files.createDirectories(destination.getParent());
                                }

                                Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                                artifacts.add(new StoredArtifact(
                                        UUID.randomUUID().toString(),
                                        relativePath.toString().replace('\\', '/'),
                                        destination
                                ));
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to store artifact: " + file, e);
                            }
                        });
            }

            return artifacts;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read sandbox artifacts", e);
        }
    }
}