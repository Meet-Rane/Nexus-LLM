package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ReadFileTool {

    private final ArtifactService artifactService;

    public ReadFileTool(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    public record ReadFileRequest(String path) {}

    @Tool(
        name = "read_file",
        description = """
            Read the text content of a file relative to the conversation workspace directory.
            Use this tool to read existing source code, configuration, or documentation files before making edits.
            """
    )
    public String readFile(ReadFileRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("read_file", "Reading file: " + request.path()));

        try {
            String content = artifactService.readFile(conversationId, request.path());
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("read_file", "Read file successfully: " + request.path()));
            return content;
        } catch (Exception e) {
            String errorMsg = "Failed to read file: " + e.getMessage();
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("read_file", errorMsg));
            return errorMsg;
        }
    }
}
