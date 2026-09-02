package com.localllm.sovereign_ai_workbench.Service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Router.ModelRouter;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionTool;
import com.localllm.sovereign_ai_workbench.Tools.CreateFileTool;
import com.localllm.sovereign_ai_workbench.Tools.ReadFileTool;
import com.localllm.sovereign_ai_workbench.Tools.WriteFileTool;
import com.localllm.sovereign_ai_workbench.Tools.ListFilesTool;

@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ModelRouter modelRouter;
    private final CodeExecutionTool codeExecutionTool;
    private final CreateFileTool createFileTool;
    private final ReadFileTool readFileTool;
    private final WriteFileTool writeFileTool;
    private final ListFilesTool listFilesTool;
    private final String provider;

    public AgentService(
            @Qualifier("chatClient") ChatClient chatClient,
            ChatMemory chatMemory,
            ModelRouter modelRouter,
            CodeExecutionTool codeExecutionTool,
            CreateFileTool createFileTool,
            ReadFileTool readFileTool,
            WriteFileTool writeFileTool,
            ListFilesTool listFilesTool,
            @Value("${ai.provider}") String provider
    ) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.modelRouter = modelRouter;
        this.codeExecutionTool = codeExecutionTool;
        this.createFileTool = createFileTool;
        this.readFileTool = readFileTool;
        this.writeFileTool = writeFileTool;
        this.listFilesTool = listFilesTool;
        this.provider = provider;
    }

    public List<Message> getChatHistory(String conversationId) {
        return chatMemory.get(conversationId);
    }

    public String chat(String conversationId, String message) {
        try {
            ConversationContextHolder.setConversationId(conversationId);

            String selectedModel = modelRouter.selectModel(conversationId, message);

            System.out.println("Provider: " + provider);
            System.out.println("Selected model: " + selectedModel);

            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(codeExecutionTool, createFileTool, readFileTool, writeFileTool, listFilesTool)
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

            return request.call().content();
        } finally {
            ConversationContextHolder.clear();
        }
    }

    public Flux<AgentStreamEvent> streamChat(String conversationId, String message) {
        return Flux.create(sink -> {
            try {
                ConversationContextHolder.setConversationId(conversationId);
                ConversationContextHolder.setEventListener(sink::next);

                String selectedModel = modelRouter.selectModel(conversationId, message);
                sink.next(AgentStreamEvent.router(selectedModel, "Selected model: " + selectedModel));

                ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .tools(codeExecutionTool, createFileTool, readFileTool, writeFileTool, listFilesTool)
                        .user(message);

                if ("ollama".equalsIgnoreCase(provider)) {
                    request.options(OllamaChatOptions.builder().model(selectedModel));
                } else if ("nvidia".equalsIgnoreCase(provider)) {
                    request.options(OpenAiChatOptions.builder().model(selectedModel));
                } else {
                    throw new IllegalArgumentException("Unsupported AI provider: " + provider);
                }

                // Execute agent request reliably (handles tool execution and avoids HTTP/2 stream cancellations)
                String responseText = request.call().content();

                // Stream response text chunks smoothly to the UI
                if (responseText != null && !responseText.isBlank()) {
                    int chunkSize = 25;
                    for (int i = 0; i < responseText.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, responseText.length());
                        sink.next(AgentStreamEvent.text(responseText.substring(i, end)));
                    }
                }

                sink.next(AgentStreamEvent.done());
                sink.complete();
            } catch (Exception e) {
                System.err.println("Agent execution error: " + e.getMessage());
                sink.next(AgentStreamEvent.error(e.getMessage() != null ? e.getMessage() : "Execution error occurred"));
                sink.complete();
            } finally {
                ConversationContextHolder.clear();
            }
        });
    }
}