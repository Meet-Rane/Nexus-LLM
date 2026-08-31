package com.localllm.sovereign_ai_workbench.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.localllm.sovereign_ai_workbench.Router.ModelRouter;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionTool;

@Service
public class AgentService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ModelRouter modelRouter;
    private final CodeExecutionTool codeExecutionTool;

    public AgentService(
        @Qualifier("chatClient") ChatClient chatClient,
        ChatMemory chatMemory,
        ModelRouter modelRouter,
        CodeExecutionTool codeExecutionTool
    ){
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.modelRouter = modelRouter;
        this.codeExecutionTool = codeExecutionTool;
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
                .tools(codeExecutionTool)
                .user(message)
                .call()
                .content();
    }
}
