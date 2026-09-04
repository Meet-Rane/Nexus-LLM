package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Dto.ArtifactDto;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import com.localllm.sovereign_ai_workbench.Service.DocumentGenerationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WriteFileTool {

    private final ArtifactService artifactService;
    private final DocumentGenerationService documentGenerationService;

    public WriteFileTool(
            ArtifactService artifactService,
            DocumentGenerationService documentGenerationService) {
        this.artifactService = artifactService;
        this.documentGenerationService = documentGenerationService;
    }

    @Tool(
        name = "write_file",
        description = """
            Update or overwrite the content of a file in the workspace.
            Use this tool to modify existing files with updated code, configurations, or text.
            """
    )
    public String writeFile(WriteFileRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        String rawPath = request.getPath() != null ? request.getPath() : "file.txt";
        String lowerPath = rawPath.toLowerCase();
        String cleanFileName = rawPath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        if (cleanFileName.contains("/")) {
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf('/') + 1);
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("write_file", "Updating file: " + cleanFileName));

        try {
            Artifact artifact;
            if (lowerPath.endsWith(".docx") || lowerPath.endsWith(".doc")) {
                byte[] bytes = documentGenerationService.generateDocx(cleanFileName, request.getContent());
                artifact = artifactService.saveFileBytes(conversationId, request.getPath(), bytes);
            } else if (lowerPath.endsWith(".pdf")) {
                byte[] bytes = documentGenerationService.generatePdf(cleanFileName, request.getContent());
                artifact = artifactService.saveFileBytes(conversationId, request.getPath(), bytes);
            } else {
                artifact = artifactService.saveFile(conversationId, request.getPath(), request.getContent());
            }

            ArtifactDto dto = ArtifactDto.fromEntity(artifact);
            String result = "File '" + artifact.getFileName() + "' updated successfully (" + artifact.getFileSize() + " bytes).";

            ConversationContextHolder.emitEvent(AgentStreamEvent.artifactCreated(dto));
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolCompleteWithArtifact("write_file", result, dto));
            
            return result;
        } catch (Exception e) {
            String errorMsg = "Failed to write file: " + e.getMessage();
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("write_file", errorMsg));
            return errorMsg;
        }
    }
}
