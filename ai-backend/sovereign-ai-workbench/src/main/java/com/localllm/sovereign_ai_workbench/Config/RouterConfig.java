package com.localllm.sovereign_ai_workbench.Config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RouterConfig {
    
    @Value("${ai.provider}")
    private String provider;

    @Bean
    public ChatClient routerClient(

            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,

            @Qualifier("openAiChatModel") ChatModel openAiChatModel) {

        ChatModel selectedChatModel;

        if ("ollama".equalsIgnoreCase(provider)) {

            selectedChatModel = ollamaChatModel;

        } else if ("nvidia".equalsIgnoreCase(provider)) {

            selectedChatModel = openAiChatModel;

        } else {

            throw new IllegalArgumentException(
                    "Unsupported AI provider: " + provider
            );
        }

        return ChatClient.builder(selectedChatModel)
                .build();
    }
}
