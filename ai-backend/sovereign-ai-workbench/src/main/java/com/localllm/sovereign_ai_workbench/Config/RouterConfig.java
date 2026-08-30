package com.localllm.sovereign_ai_workbench.Config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouterConfig {
    
    @Bean
    public ChatClient routerClient(
            OpenAiChatModel chatModel
        ) {

        return ChatClient.builder(chatModel)
                .build();
    }
}
