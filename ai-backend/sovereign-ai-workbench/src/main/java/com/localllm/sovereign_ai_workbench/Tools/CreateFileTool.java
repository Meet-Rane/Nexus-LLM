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
public class CreateFileTool {

    private final ArtifactService artifactService;
    private final DocumentGenerationService documentGenerationService;

    public CreateFileTool(
            ArtifactService artifactService,
            DocumentGenerationService documentGenerationService) {
        this.artifactService = artifactService;
        this.documentGenerationService = documentGenerationService;
    }

    @Tool(
        name = "create_file",
        description = """
            Create a new file with the specified filename and content.
            Use this tool when creating code files, text documents, CSV datasets, or configuration files.
            Pass simple filenames such as 'intrusion_dataset.csv', 'reboiler_calc.py', or 'report.docx'.
            """
    )
    public String createFile(CreateFileRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        String rawPath = request.getPath() != null ? request.getPath() : "untitled.txt";
        String lowerPath = rawPath.toLowerCase();
        String cleanFileName = rawPath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        if (cleanFileName.contains("/")) {
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf('/') + 1);
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("create_file", "Creating file: " + cleanFileName));

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
            String result = "File '" + artifact.getFileName() + "' created successfully (" + artifact.getFileSize() + " bytes).";

            ConversationContextHolder.emitEvent(AgentStreamEvent.artifactCreated(dto));
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolCompleteWithArtifact("create_file", result, dto));
            
            return result;
        } catch (Exception e) {
            String errorMsg = "Failed to create file: " + e.getMessage();
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("create_file", errorMsg));
            return errorMsg;
        }
    }
}
