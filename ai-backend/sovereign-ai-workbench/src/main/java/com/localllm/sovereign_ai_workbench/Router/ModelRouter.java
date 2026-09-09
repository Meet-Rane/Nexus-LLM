package com.localllm.sovereign_ai_workbench.Router;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ModelRouter {

    private final String codingModel;
    private final String generalModel;
    private final String reasoningModel;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ModelRouter(
        @Qualifier("routerClient") ChatClient chatClient,
        ChatMemory chatMemory,
        @Value("${ai.coding.model:nvidia/nemotron-3.5-lightning-30b-a3b}") String codingModel,
        @Value("${ai.general.model:nvidia/nemotron-3.5-lightning-30b-a3b}") String generalModel,
        @Value("${ai.reasoning.model:nvidia/nemotron-3.5-lightning-30b-a3b}") String reasoningModel
    ){
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.codingModel = codingModel;
        this.generalModel = generalModel;
        this.reasoningModel = reasoningModel != null && !reasoningModel.isBlank() ? reasoningModel : generalModel;
    }

    private String getConversationHistory(String conversationId) {
        try {
            return chatMemory
                    .get(conversationId)
                    .stream()
                    .map(message -> message.getMessageType() + ": " + message.getText())
                    .reduce("", (history, message) -> history + "\n" + message);
        } catch (Exception e) {
            return "";
        }
    }

    public RouteDecision selectModel(String conversationId, String message) {
        String lowerMessage = message != null ? message.toLowerCase() : "";

        // 1. Fast Pattern Check for Coding & Tool Development
        if (lowerMessage.contains("code") || lowerMessage.contains("python") || 
            lowerMessage.contains("script") || lowerMessage.contains("function") ||
            lowerMessage.contains("fastapi") || lowerMessage.contains("bug") || 
            lowerMessage.contains("class") || lowerMessage.contains("method") ||
            lowerMessage.contains("endpoint") || lowerMessage.contains("csv") ||
            lowerMessage.contains("dataset") || lowerMessage.contains("sql") ||
            lowerMessage.contains("program") || lowerMessage.contains("docker")) {
            return new RouteDecision(
                    codingModel,
                    "CODING",
                    "Selected coding specialist: " + codingModel + " (Trigger: software engineering, script automation & data generation)"
            );
        }

        // 2. Fast Pattern Check for Engineering Calculations & Process Reasoning
        if (lowerMessage.contains("calculate") || lowerMessage.contains("lmtd") ||
            lowerMessage.contains("heat exchanger") || lowerMessage.contains("distillation") ||
            lowerMessage.contains("reboiler") || lowerMessage.contains("thermodynamic") ||
            lowerMessage.contains("mass balance") || lowerMessage.contains("pressure drop") ||
            lowerMessage.contains("flow rate") || lowerMessage.contains("reynolds")) {
            return new RouteDecision(
                    reasoningModel,
                    "CALCULATION_REASONING",
                    "Selected reasoning & engineering specialist: " + reasoningModel + " (Trigger: refinery calculations & process analysis)"
            );
        }

        // 3. Fast Pattern Check for Structured Industrial Documents & Approval Notes
        if (lowerMessage.contains("approval note") || lowerMessage.contains("board presentation") ||
            lowerMessage.contains("inspection report") || lowerMessage.contains("sop") ||
            lowerMessage.contains("memo") || lowerMessage.contains("standard operating procedure") ||
            lowerMessage.contains("pdf guide") || lowerMessage.contains("word document")) {
            return new RouteDecision(
                    generalModel,
                    "DOCUMENT_APPROVAL",
                    "Selected document synthesis model: " + generalModel + " (Trigger: industrial deliverable & formal documentation drafting)"
            );
        }

        // 4. LLM-Based Contextual Intent Classification
        try {
            String conversationHistory = getConversationHistory(conversationId);

            String systemMessage = """
                    You are an intelligent AI model router for an industrial PSU / refinery workbench.
                    Categorize the user's intent to route to the optimal on-premise model.

                    Categories:
                    - CODING: software, script, programming, database queries, dataset creation
                    - REASONING: engineering calculations, mass/heat balance, numerical analysis
                    - GENERAL: explanations, summaries, policy inquiries, conversation

                    CONVERSATION CONTEXT:
                    %s

                    LATEST REQUEST:
                    %s

                    Respond with ONLY one word: CODING, REASONING, or GENERAL.
                    """.formatted(conversationHistory, message);

            String decision = chatClient.prompt()
                    .user(systemMessage)
                    .call()
                    .content();

            if (decision != null) {
                String upper = decision.trim().toUpperCase();
                if (upper.contains("CODING")) {
                    return new RouteDecision(
                            codingModel,
                            "CODING",
                            "Selected coding specialist: " + codingModel + " (Intent: contextual code & script assistance)"
                    );
                } else if (upper.contains("REASONING")) {
                    return new RouteDecision(
                            reasoningModel,
                            "CALCULATION_REASONING",
                            "Selected reasoning model: " + reasoningModel + " (Intent: complex process engineering & reasoning)"
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("ModelRouter LLM evaluation skipped/failed: " + e.getMessage());
        }

        // 5. Default Generic Model Selection
        return new RouteDecision(
                generalModel,
                "GENERAL",
                "Selected default general-purpose model: " + generalModel + " (General industrial assistance, knowledge query & reasoning)"
        );
    } 
}
