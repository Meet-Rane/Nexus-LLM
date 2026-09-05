package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Service.NetworkAuditService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class KnowledgeSearchTool {

    private final RestClient ragClient;
    private final String ragBaseUrl;
    private final NetworkAuditService networkAuditService;

    public KnowledgeSearchTool(
            @Value("${rag.service.base-url:http://localhost:8001}") String baseUrl,
            NetworkAuditService networkAuditService
    ) {
        this.ragClient = RestClient.builder().baseUrl(baseUrl).build();
        this.ragBaseUrl = baseUrl;
        this.networkAuditService = networkAuditService;
    }

    public record KnowledgeSearchRequest(String query, Integer topK) {}

    private record KnowledgeSearchResponse(List<String> chunks, List<String> sources) {}

    @Tool(
        name = "search_knowledge_base",
        description = """
            Search the organization's local RAG knowledge base for relevant passages from SOPs,
            manuals, inspection reports, and other indexed documents. Use this before answering
            questions that depend on internal documents. Provide a focused query and topK from 1 to 10.
            """
    )
    public String searchKnowledgeBase(KnowledgeSearchRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            return "Knowledge base search failed: query cannot be empty.";
        }

        int topK = request.topK() == null ? 5 : Math.max(1, Math.min(request.topK(), 10));
        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart(
                "search_knowledge_base",
                "Searching local knowledge base"
        ));

        try {
            networkAuditService.record("RAG", ragBaseUrl, "retrieve · " + request.query());
            KnowledgeSearchResponse response = ragClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/retrieve")
                            .queryParam("query", request.query())
                            .queryParam("top_k", topK)
                            .build())
                    .retrieve()
                    .body(KnowledgeSearchResponse.class);

            if (response == null || response.chunks() == null || response.chunks().isEmpty()) {
                String result = "No relevant passages were found in the local knowledge base.";
                ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("search_knowledge_base", result));
                return result;
            }

            if (response.sources() != null) {
                ConversationContextHolder.setKnowledgeSources(
                        response.sources().stream()
                                .filter(source -> source != null && !source.isBlank())
                                .distinct()
                                .toList()
                );
            }

            StringBuilder result = new StringBuilder("Local knowledge base results:\n");
            for (int i = 0; i < response.chunks().size(); i++) {
                String source = response.sources() != null && i < response.sources().size()
                        ? response.sources().get(i)
                        : "unknown";
                result.append("\n[").append(i + 1).append("] Source: ").append(source)
                        .append("\n").append(response.chunks().get(i)).append("\n");
            }
            result.append("\nCitation requirement: include the relevant source filename(s) in the final answer.");

            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete(
                    "search_knowledge_base",
                    "Retrieved " + response.chunks().size() + " local passages"
            ));
            return result.toString();
        } catch (Exception e) {
            String result = "Knowledge base search failed: " + e.getMessage();
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("search_knowledge_base", result));
            return result;
        }
    }
}
