package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListFilesTool {

    private final ArtifactService artifactService;

    public ListFilesTool(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    public record ListFilesRequest() {}

    @Tool(
        name = "list_files",
        description = """
            List all files and paths currently created in the conversation workspace.
            Use this tool to explore existing files before adding or updating files.
            """
    )
    public String listFiles(ListFilesRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("list_files", "Listing workspace files"));

        try {
            List<Artifact> artifacts = artifactService.listArtifacts(conversationId);
            if (artifacts.isEmpty()) {
                String emptyMsg = "Workspace is empty. No files exist yet.";
                ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("list_files", emptyMsg));
                return emptyMsg;
            }

            String result = artifacts.stream()
                    .map(a -> a.getFilePath() + " (" + a.getArtifactType() + ", " + a.getFileSize() + " bytes)")
                    .collect(Collectors.joining("\n"));
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("list_files", "Found " + artifacts.size() + " files"));
            return result;
        } catch (Exception e) {
            String errorMsg = "Failed to list files: " + e.getMessage();
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("list_files", errorMsg));
            return errorMsg;
        }
    }
}
