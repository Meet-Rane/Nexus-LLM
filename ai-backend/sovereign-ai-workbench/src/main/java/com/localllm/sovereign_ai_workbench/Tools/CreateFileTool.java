package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Dto.ArtifactDto;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CreateFileTool {

    private final ArtifactService artifactService;

    public CreateFileTool(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    public record CreateFileRequest(String path, String content) {}

    @Tool(
        name = "create_file",
        description = """
            Create a new file with the specified path and content relative to the conversation workspace directory.
            Automatically creates parent folders if necessary.
            
            Use this tool when creating code files, text documents, or configuration files.
            Do NOT provide absolute paths (e.g. C:\\ or /usr/); use relative paths like 'main.py' or 'src/app.py'.
            """
    )
    public String createFile(CreateFileRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("create_file", "Creating file: " + request.path()));

        try {
            Artifact artifact = artifactService.saveFile(conversationId, request.path(), request.content());
            ArtifactDto dto = ArtifactDto.fromEntity(artifact);
            String result = "File created successfully: " + artifact.getFilePath();

            // Emit artifact creation event for frontend rendering
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
