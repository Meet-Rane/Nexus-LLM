package com.localllm.sovereign_ai_workbench.Dto;

import com.localllm.sovereign_ai_workbench.Entity.Artifact;

public record ArtifactDto(
        Long id,
        String conversationId,
        String fileName,
        String path,
        String artifactType,
        long fileSize,
        String createdAt,
        String updatedAt
) {
    public static ArtifactDto fromEntity(Artifact artifact) {
        return new ArtifactDto(
                artifact.getId(),
                artifact.getConversationId(),
                artifact.getFileName(),
                artifact.getFilePath(),
                artifact.getArtifactType(),
                artifact.getFileSize(),
                artifact.getCreatedAt() != null ? artifact.getCreatedAt().toString() : null,
                artifact.getUpdatedAt() != null ? artifact.getUpdatedAt().toString() : null
        );
    }
}
