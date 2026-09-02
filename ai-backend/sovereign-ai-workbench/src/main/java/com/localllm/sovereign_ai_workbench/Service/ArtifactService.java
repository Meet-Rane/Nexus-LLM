package com.localllm.sovereign_ai_workbench.Service;

import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Repo.ArtifactRepository;
import com.localllm.sovereign_ai_workbench.Tools.ArtifactStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class ArtifactService {

    private final ArtifactStorageService artifactStorageService;
    private final ArtifactRepository artifactRepository;

    public ArtifactService(
            ArtifactStorageService artifactStorageService,
            ArtifactRepository artifactRepository) {
        this.artifactStorageService = artifactStorageService;
        this.artifactRepository = artifactRepository;
    }

    @Transactional
    public Artifact saveFile(String conversationId, String relativePath, String content) {
        String cleanPath = relativePath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        Path savedPath = artifactStorageService.saveFileContent(conversationId, cleanPath, content);
        
        long fileSize = 0;
        try {
            fileSize = java.nio.file.Files.size(savedPath);
        } catch (Exception ignored) {
        }

        String fileName = savedPath.getFileName().toString();
        String artifactType = determineArtifactType(fileName);

        Optional<Artifact> existing = artifactRepository.findByConversationIdAndFilePath(conversationId, cleanPath);
        Artifact artifact;
        if (existing.isPresent()) {
            artifact = existing.get();
            artifact.setFileName(fileName);
            artifact.setArtifactType(artifactType);
            artifact.setFileSize(fileSize);
        } else {
            artifact = Artifact.builder()
                    .conversationId(conversationId)
                    .fileName(fileName)
                    .filePath(cleanPath)
                    .artifactType(artifactType)
                    .fileSize(fileSize)
                    .build();
        }

        return artifactRepository.save(artifact);
    }

    @Transactional
    public Artifact saveFileBytes(String conversationId, String relativePath, byte[] bytes) {
        String cleanPath = relativePath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        Path savedPath = artifactStorageService.saveFileBytes(conversationId, cleanPath, bytes);
        
        long fileSize = bytes != null ? bytes.length : 0;
        String fileName = savedPath.getFileName().toString();
        String artifactType = determineArtifactType(fileName);

        Optional<Artifact> existing = artifactRepository.findByConversationIdAndFilePath(conversationId, cleanPath);
        Artifact artifact;
        if (existing.isPresent()) {
            artifact = existing.get();
            artifact.setFileName(fileName);
            artifact.setArtifactType(artifactType);
            artifact.setFileSize(fileSize);
        } else {
            artifact = Artifact.builder()
                    .conversationId(conversationId)
                    .fileName(fileName)
                    .filePath(cleanPath)
                    .artifactType(artifactType)
                    .fileSize(fileSize)
                    .build();
        }

        return artifactRepository.save(artifact);
    }

    public String readFile(String conversationId, String relativePath) {
        String cleanPath = relativePath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        return artifactStorageService.readFileContent(conversationId, cleanPath);
    }

    public byte[] getFileBytes(String conversationId, String relativePath) {
        String cleanPath = relativePath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        return artifactStorageService.readFileBytes(conversationId, cleanPath);
    }

    public List<Artifact> listArtifacts(String conversationId) {
        return artifactRepository.findByConversationId(conversationId);
    }

    @Transactional
    public boolean deleteFile(String conversationId, String relativePath) {
        String cleanPath = relativePath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        boolean deletedFromDisk = artifactStorageService.deleteFile(conversationId, cleanPath);
        artifactRepository.deleteByConversationIdAndFilePath(conversationId, cleanPath);
        return deletedFromDisk;
    }

    public Optional<Artifact> getArtifactByPath(String conversationId, String relativePath) {
        String cleanPath = relativePath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        return artifactRepository.findByConversationIdAndFilePath(conversationId, cleanPath);
    }

    private String determineArtifactType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".py")) return "PYTHON";
        if (lower.endsWith(".java")) return "JAVA";
        if (lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".jsx") || lower.endsWith(".tsx")) return "JAVASCRIPT";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "HTML";
        if (lower.endsWith(".css")) return "CSS";
        if (lower.endsWith(".json")) return "JSON";
        if (lower.endsWith(".xml")) return "XML";
        if (lower.endsWith(".md")) return "MARKDOWN";
        if (lower.endsWith(".txt")) return "TEXT";
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "WORD";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv")) return "EXCEL";
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) return "POWERPOINT";
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".svg")) return "IMAGE";
        return "UNKNOWN";
    }
}
