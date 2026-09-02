package com.localllm.sovereign_ai_workbench.Controller;

import com.localllm.sovereign_ai_workbench.Dto.ArtifactDto;
import com.localllm.sovereign_ai_workbench.Dto.ArtifactFileContentDto;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/conversations/{conversationId}/artifacts")
public class ArtifactController {

    private final ArtifactService artifactService;

    public ArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping
    public ResponseEntity<List<ArtifactDto>> listArtifacts(
            @PathVariable String conversationId) {

        List<ArtifactDto> dtos = artifactService.listArtifacts(conversationId).stream()
                .map(ArtifactDto::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/content")
    public ResponseEntity<ArtifactFileContentDto> getFileContent(
            @PathVariable String conversationId,
            @RequestParam("path") String relativePath) {

        String content = artifactService.readFile(conversationId, relativePath);
        Optional<Artifact> artifactOpt = artifactService.getArtifactByPath(conversationId, relativePath);

        String fileName = artifactOpt.map(Artifact::getFileName)
                .orElseGet(() -> {
                    int lastSlash = relativePath.lastIndexOf('/');
                    return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
                });

        return ResponseEntity.ok(new ArtifactFileContentDto(fileName, relativePath, content));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String conversationId,
            @RequestParam("path") String relativePath) {

        byte[] data = artifactService.getFileBytes(conversationId, relativePath);
        ByteArrayResource resource = new ByteArrayResource(data);

        Optional<Artifact> artifactOpt = artifactService.getArtifactByPath(conversationId, relativePath);
        String fileName = artifactOpt.map(Artifact::getFileName)
                .orElseGet(() -> {
                    int lastSlash = relativePath.lastIndexOf('/');
                    return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
                });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

    @DeleteMapping
    public ResponseEntity<String> deleteArtifact(
            @PathVariable String conversationId,
            @RequestParam("path") String relativePath) {

        boolean deleted = artifactService.deleteFile(conversationId, relativePath);
        if (deleted) {
            return ResponseEntity.ok("Artifact deleted successfully.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
