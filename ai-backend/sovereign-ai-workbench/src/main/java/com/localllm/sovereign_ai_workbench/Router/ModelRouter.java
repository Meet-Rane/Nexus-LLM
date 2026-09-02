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
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ModelRouter(
        @Qualifier("routerClient") ChatClient chatClient,
        ChatMemory chatMemory,
        @Value("${ai.coding.model:meta/llama-3.3-70b-instruct}") String codingModel,
        @Value("${ai.general.model:meta/llama-3.3-70b-instruct}") String generalModel
    ){
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.codingModel = codingModel;
        this.generalModel = generalModel;
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

    public String selectModel(String conversationId, String message) {
        String lowerMessage = message != null ? message.toLowerCase() : "";
        if (lowerMessage.contains("code") || lowerMessage.contains("function") || 
            lowerMessage.contains("python") || lowerMessage.contains("script") ||
            lowerMessage.contains("fastapi") || lowerMessage.contains("bug") || 
            lowerMessage.contains("class") || lowerMessage.contains("method") ||
            lowerMessage.contains("create") || lowerMessage.contains("write")) {
            return codingModel;
        }

        try {
            String conversationHistory = getConversationHistory(conversationId);

            String systemMessage = """
                    You are an AI model router.
                    Your job is to decide which model should handle the user's request.

                    Analyze BOTH:
                    1. The previous conversation context
                    2. The latest user message

                    Choose exactly one category:
                    CODING
                    GENERAL

                    PREVIOUS CONVERSATION:
                    %s

                    LATEST USER MESSAGE:
                    %s

                    Return ONLY one word: CODING or GENERAL.
                    """.formatted(conversationHistory, message);

            String decision = chatClient.prompt()
                    .user(systemMessage)
                    .call()
                    .content();

            if (decision != null && decision.trim().toUpperCase().contains("CODING")) {
                return codingModel;
            }
        } catch (Exception e) {
            System.err.println("ModelRouter LLM evaluation failed: " + e.getMessage() + ". Defaulting to general model: " + generalModel);
        }

        return generalModel;
    } 
}
