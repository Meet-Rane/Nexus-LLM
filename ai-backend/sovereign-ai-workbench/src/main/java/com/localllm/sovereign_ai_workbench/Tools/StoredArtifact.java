package com.localllm.sovereign_ai_workbench.Tools;

import java.nio.file.Path;

public record StoredArtifact(
        String id,
        String name,
        Path path
) {
}