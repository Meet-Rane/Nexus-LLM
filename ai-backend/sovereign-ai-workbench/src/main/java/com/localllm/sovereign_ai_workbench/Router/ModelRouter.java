package com.localllm.sovereign_ai_workbench.Router;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ModelRouter {

    public static final String CODING_MODEL =
            "nvidia/nemotron-3-ultra-550b-a55b";

    public static final String GENERAL_MODEL =
            "nvidia/nemotron-3-ultra-550b-a55b";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ModelRouter(
        @Qualifier("routerClient") ChatClient chatClient,
        ChatMemory chatMemory
    ){
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    private String getConversationHistory(String conversationId) {

        return chatMemory
                .get(conversationId)
                .stream()
                .map(message ->
                        message.getMessageType()
                                + ": "
                                + message.getText())
                .reduce(
                        "",
                        (history, message) ->
                                history + "\n" + message
                );
    }

    public String selectModel(String conversationId, String message){

        String conversationHistory = getConversationHistory(conversationId);

        String systemMessage="""
                You are an AI model router.

                Your job is to decide which model should handle the user's request.

                Analyze BOTH:
                1. The previous conversation context
                2. The latest user message

                Choose exactly one category:

                CODING
                GENERAL

                Choose CODING when the user's request primarily involves:
                - writing code
                - modifying code
                - debugging
                - programming
                - software development
                - APIs
                - databases
                - frameworks
                - algorithms
                - technical implementation

                Choose GENERAL for everything else.

                Consider the conversation context carefully. The latest message
                may depend on a previous coding discussion.

                PREVIOUS CONVERSATION:
                %s

                LATEST USER MESSAGE:
                %s

                Return ONLY one word:

                CODING

                or

                GENERAL.
                """.formatted(conversationHistory, message);
        
        String decision=chatClient.prompt()
                            .user(systemMessage)
                            .call()
                            .content();

        decision = decision.trim().toUpperCase();
        if(decision.contains("CODING")){
            return CODING_MODEL;
        }
        return GENERAL_MODEL;
    } 
}
