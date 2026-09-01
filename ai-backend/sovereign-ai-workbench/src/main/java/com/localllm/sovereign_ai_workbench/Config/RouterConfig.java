package com.localllm.sovereign_ai_workbench.Config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouterConfig {
    
    @Bean
    public ChatClient routerClient(
            @Qualifier("ollamaChatModel")  ChatModel chatModel
        ) {

        return ChatClient.builder(chatModel)
                .build();
    }
}
