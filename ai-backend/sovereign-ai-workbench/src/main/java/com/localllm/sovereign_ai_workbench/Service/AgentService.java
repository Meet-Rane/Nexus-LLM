package com.localllm.sovereign_ai_workbench.Service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.localllm.sovereign_ai_workbench.Router.ModelRouter;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionTool;

@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ModelRouter modelRouter;
    private final CodeExecutionTool codeExecutionTool;
    private final String provider;

    public AgentService(

            @Qualifier("chatClient") ChatClient chatClient,

            ChatMemory chatMemory,

            ModelRouter modelRouter,

            CodeExecutionTool codeExecutionTool,

            @Value("${ai.provider}") String provider
    ) {

        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.modelRouter = modelRouter;
        this.codeExecutionTool = codeExecutionTool;
        this.provider = provider;
    }

    public List<Message> getChatHistory(String conversationId) {

        return chatMemory.get(conversationId);
    }

    public String chat(String conversationId, String message) {

        String selectedModel =
                modelRouter.selectModel(conversationId, message);

        System.out.println("Provider: " + provider);
        System.out.println("Selected model: " + selectedModel);

        ChatClient.ChatClientRequestSpec request =
                chatClient.prompt()
                        .advisors(advisor -> advisor
                                .param(ChatMemory.CONVERSATION_ID,
                                        conversationId))
                        .tools(codeExecutionTool)
                        .user(message);

        if ("ollama".equalsIgnoreCase(provider)) {

            request.options(
                    OllamaChatOptions.builder()
                            .model(selectedModel)
            );

        } else if ("nvidia".equalsIgnoreCase(provider)) {

            request.options(
                    OpenAiChatOptions.builder()
                            .model(selectedModel)
            );

        } else {

            throw new IllegalArgumentException(
                    "Unsupported AI provider: " + provider
            );
        }

        return request
                .call()
                .content();
    }
}