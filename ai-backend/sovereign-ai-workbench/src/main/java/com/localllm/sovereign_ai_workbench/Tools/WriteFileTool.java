package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Dto.ArtifactDto;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WriteFileTool {

    private final ArtifactService artifactService;

    public WriteFileTool(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    public record WriteFileRequest(String path, String content) {}

    @Tool(
        name = "write_file",
        description = """
            Update or overwrite the content of a file relative to the conversation workspace directory.
            Use this tool to modify existing files with updated code or text.
            """
    )
    public String writeFile(WriteFileRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("write_file", "Updating file: " + request.path()));

        try {
            Artifact artifact = artifactService.saveFile(conversationId, request.path(), request.content());
            ArtifactDto dto = ArtifactDto.fromEntity(artifact);
            String result = "File updated successfully: " + artifact.getFilePath();

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
