package com.localllm.sovereign_ai_workbench.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artifacts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"conversationId", "filePath"})
})
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String artifactType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Artifact() {
    }

    public Artifact(Long id, String conversationId, String fileName, String filePath, String artifactType, long fileSize, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.artifactType = artifactType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static ArtifactBuilder builder() {
        return new ArtifactBuilder();
    }

    public static class ArtifactBuilder {
        private Long id;
        private String conversationId;
        private String fileName;
        private String filePath;
        private String artifactType;
        private long fileSize;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ArtifactBuilder id(Long id) { this.id = id; return this; }
        public ArtifactBuilder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
        public ArtifactBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public ArtifactBuilder filePath(String filePath) { this.filePath = filePath; return this; }
        public ArtifactBuilder artifactType(String artifactType) { this.artifactType = artifactType; return this; }
        public ArtifactBuilder fileSize(long fileSize) { this.fileSize = fileSize; return this; }
        public ArtifactBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ArtifactBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Artifact build() {
            return new Artifact(id, conversationId, fileName, filePath, artifactType, fileSize, createdAt, updatedAt);
        }
    }
}
