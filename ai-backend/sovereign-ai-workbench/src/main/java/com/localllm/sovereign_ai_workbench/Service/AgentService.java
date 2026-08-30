package com.localllm.sovereign_ai_workbench.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.localllm.sovereign_ai_workbench.Router.ModelRouter;

@Service
public class AgentService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ModelRouter modelRouter;

    public AgentService(
        @Qualifier("chatClient") ChatClient chatClient,
        ChatMemory chatMemory,
        ModelRouter modelRouter
    ){
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.modelRouter = modelRouter;
    }


    public String chat(String conversationId, String message){

        String selectedModel=modelRouter.selectModel(conversationId, message);
        System.out.println(selectedModel);

        return chatClient.prompt()
                .advisors(advisor -> advisor
                        .param(chatMemory.CONVERSATION_ID, conversationId))
                .options(
                    OpenAiChatOptions.builder()
                        .model(selectedModel)
                )
                .user(message)
                .call()
                .content();
    }
}
