package com.localllm.sovereign_ai_workbench.Tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public List<StoredArtifact> storeArtifacts(
            Path outputDirectory,
            String executionId) {

        List<StoredArtifact> artifacts = new ArrayList<>();

        try {

            if (!Files.exists(outputDirectory)) {
                return artifacts;
            }

            Path executionDirectory =
                    storageRoot.resolve(executionId);

            Files.createDirectories(executionDirectory);

            Files.walk(outputDirectory)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {

                        try {

                            Path relativePath =
                                    outputDirectory.relativize(file);

                            Path destination =
                                    executionDirectory
                                            .resolve(relativePath)
                                            .normalize();

                            if (!destination.startsWith(
                                    executionDirectory)) {
                                throw new SecurityException(
                                        "Invalid artifact path"
                                );
                            }

                            if (destination.getParent() != null) {
                                Files.createDirectories(
                                        destination.getParent()
                                );
                            }

                            Files.copy(
                                    file,
                                    destination,
                                    StandardCopyOption.REPLACE_EXISTING
                            );

                            artifacts.add(
                                    new StoredArtifact(
                                            UUID.randomUUID().toString(),
                                            relativePath.toString(),
                                            destination
                                    )
                            );

                        } catch (IOException e) {
                            throw new RuntimeException(
                                    "Failed to store artifact: "
                                            + file,
                                    e
                            );
                        }
                    });

            return artifacts;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to read sandbox artifacts",
                    e
            );
        }
    }
}