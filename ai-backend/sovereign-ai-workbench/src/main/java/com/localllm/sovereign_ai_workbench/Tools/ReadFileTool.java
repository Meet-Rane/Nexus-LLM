package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ReadFileTool {

    private final ArtifactService artifactService;
    private static final int MAX_READ_CHARS = 8000;

    public ReadFileTool(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @Tool(
        name = "read_file",
        description = """
            Read the text content of a file in the workspace.
            Use this tool to read existing source code, datasets, configuration, or documentation files before making edits.
            """
    )
    public String readFile(ReadFileRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        String rawPath = request.getPath() != null ? request.getPath() : "file.txt";
        String cleanFileName = rawPath.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        if (cleanFileName.contains("/")) {
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf('/') + 1);
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("read_file", "Reading file: " + cleanFileName));

        try {
            String content = artifactService.readFile(conversationId, request.getPath());
            if (content == null) {
                String notFound = "File '" + cleanFileName + "' not found.";
                ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("read_file", notFound));
                return notFound;
            }

            // Truncation protection to protect the LLM context window
            if (content.length() > MAX_READ_CHARS) {
                content = content.substring(0, MAX_READ_CHARS) + "\n\n[... Remaining content truncated for LLM context window protection ...]";
            }

            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("read_file", "Read file '" + cleanFileName + "' successfully."));
            return content;
        } catch (Exception e) {
            String errorMsg = "Failed to read file: " + e.getMessage();
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("read_file", errorMsg));
            return errorMsg;
        }
    }
}
