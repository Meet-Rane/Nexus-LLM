package com.localllm.sovereign_ai_workbench.Service;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.localllm.sovereign_ai_workbench.Router.RouteDecision;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionRequest;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionTool;
import com.localllm.sovereign_ai_workbench.Tools.CreateDocumentRequest;
import com.localllm.sovereign_ai_workbench.Tools.CreateDocumentTool;
import com.localllm.sovereign_ai_workbench.Tools.CreateFileRequest;
import com.localllm.sovereign_ai_workbench.Tools.CreateFileTool;
import com.localllm.sovereign_ai_workbench.Tools.ListFilesTool;
import com.localllm.sovereign_ai_workbench.Tools.ReadFileRequest;
import com.localllm.sovereign_ai_workbench.Tools.ReadFileTool;
import com.localllm.sovereign_ai_workbench.Tools.WriteFileRequest;
import com.localllm.sovereign_ai_workbench.Tools.WriteFileTool;

@Service
public class AgentService {

    private static final String SYSTEM_PROMPT = """
            You are the Sovereign On-Premise Industrial AI Assistant for Mangalore Refinery and Petrochemicals Limited (MRPL).
            You assist refinery engineers, operations staff, and management with confidential industrial workflows: technical calculations, approval notes, inspection reports, script development, and formatted documentation.

            CRITICAL OPERATING DIRECTIVES:
            1. FILE NAMES & PATH CONCEALMENT:
               - NEVER expose raw internal storage or server directory paths (such as 'output/filename.ext' or 'storage/artifacts/...').
               - Always refer to generated or modified files strictly by their simple file name (e.g., 'intrusion_dataset.csv' or 'reboiler_spec.pdf').
               - The workbench UI automatically renders interactive download cards for all created artifacts.
            2. CONCISE & EXECUTIVE DELIVERABLES:
               - When you create or update a file or document via a tool, DO NOT paste the entire raw file text into the chat.
               - Provide a clear, structured summary of the created artifact, key technical highlights, assumptions, and findings.
            3. ENGINEERING RIGOR:
               - Show step-by-step engineering calculations with explicit formulas, input parameters, and standard engineering units (°C, bar, kg/h, kW, cSt, MW).
            4. FORMAL INDUSTRIAL DOCUMENTS:
               - When drafting approval notes or memos, use structured industrial sections: Subject, Background, Technical Evaluation, Safety & Compliance, and Recommendation.
            5. TOOL SELECTION RULES:
               - FOR GENERATING PDF OR WORD DOCUMENTS (.pdf, .docx): ALWAYS invoke the 'create_formatted_document' tool with the structured markdown content. DO NOT write or execute Python scripts to generate documents.
               - FOR COMPUTATIONAL SIMULATIONS & CODE EXECUTION: Use 'execute_python_code' (pass code in 'files' map and set 'entryFile').
               - FOR SAVING DATA & SOURCE CODE FILES (.py, .csv, .json, .sql, .txt): Use 'create_file'.
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ModelRouter modelRouter;
    private final CodeExecutionTool codeExecutionTool;
    private final CreateFileTool createFileTool;
    private final ReadFileTool readFileTool;
    private final WriteFileTool writeFileTool;
    private final ListFilesTool listFilesTool;
    private final CreateDocumentTool createDocumentTool;
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
            CreateDocumentTool createDocumentTool,
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
        this.createDocumentTool = createDocumentTool;
        this.provider = provider;
    }

    public List<Message> getChatHistory(String conversationId) {
        return chatMemory.get(conversationId);
    }

    public String chat(String conversationId, String message) {
        try {
            ConversationContextHolder.setConversationId(conversationId);

            RouteDecision decision = modelRouter.selectModel(conversationId, message);
            String selectedModel = decision.model();

            System.out.println("Provider: " + provider);
            System.out.println("Selected model: " + selectedModel + " (" + decision.category() + ")");

            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(codeExecutionTool, createFileTool, readFileTool, writeFileTool, listFilesTool, createDocumentTool)
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

            String responseText = request.call().content();
            return handleTextSimulatedToolCalls(conversationId, responseText);
        } finally {
            ConversationContextHolder.clear();
        }
    }

    public Flux<AgentStreamEvent> streamChat(String conversationId, String message) {
        return Flux.create(sink -> {
            try {
                ConversationContextHolder.setConversationId(conversationId);
                ConversationContextHolder.setEventListener(sink::next);

                RouteDecision decision = modelRouter.selectModel(conversationId, message);
                String selectedModel = decision.model();

                // Announce router decision with full reasoning to the UI stream
                sink.next(AgentStreamEvent.router(selectedModel, decision.reason()));

                ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .tools(codeExecutionTool, createFileTool, readFileTool, writeFileTool, listFilesTool, createDocumentTool)
                        .user(message);

                if ("ollama".equalsIgnoreCase(provider)) {
                    request.options(OllamaChatOptions.builder().model(selectedModel));
                } else if ("nvidia".equalsIgnoreCase(provider)) {
                    request.options(OpenAiChatOptions.builder().model(selectedModel));
                } else {
                    throw new IllegalArgumentException("Unsupported AI provider: " + provider);
                }

                // Execute agent request reliably
                String responseText = request.call().content();

                // Intercept simulated markdown tool calls generated by open-weight models
                responseText = handleTextSimulatedToolCalls(conversationId, responseText);

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

    /**
     * Auto-recovery interceptor for smaller open-weight models that occasionally output 
     * markdown JSON tool call blocks in their text response instead of native tool tokens.
     */
    private String handleTextSimulatedToolCalls(String conversationId, String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return responseText;
        }

        if (responseText.contains("create_formatted_document") || responseText.contains("create_file") || responseText.contains("\"parameters\"")) {
            try {
                String jsonCandidate = responseText;
                if (jsonCandidate.contains("```")) {
                    int start = jsonCandidate.indexOf("```");
                    int end = jsonCandidate.lastIndexOf("```");
                    if (start >= 0 && end > start) {
                        String inner = jsonCandidate.substring(start + 3, end).trim();
                        if (inner.toLowerCase().startsWith("json")) {
                            inner = inner.substring(4).trim();
                        }
                        if (inner.startsWith("{")) {
                            jsonCandidate = inner;
                        }
                    }
                }

                if (jsonCandidate.trim().startsWith("{")) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
                    mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
                    JsonNode root = mapper.readTree(jsonCandidate.trim());

                    String toolName = root.has("name") ? root.get("name").asText() : "";
                    JsonNode paramsNode = root.has("parameters") ? root.get("parameters") : root;

                    if (toolName.contains("create_formatted_document") || (paramsNode.has("content") && (paramsNode.has("title") || paramsNode.has("path")))) {
                        String p = paramsNode.has("path") ? paramsNode.get("path").asText() : "document.pdf";
                        String t = paramsNode.has("title") ? paramsNode.get("title").asText() : "MRPL Deliverable";
                        String c = paramsNode.has("content") ? paramsNode.get("content").asText() : "";

                        if (!c.isBlank()) {
                            CreateDocumentRequest docReq = new CreateDocumentRequest(p, t, c);
                            createDocumentTool.createFormattedDocument(docReq);
                            return "I have generated the formatted document '" + p + "' as requested.";
                        }
                    } else if (toolName.contains("create_file") || (paramsNode.has("path") && paramsNode.has("content"))) {
                        String p = paramsNode.has("path") ? paramsNode.get("path").asText() : "file.txt";
                        String c = paramsNode.has("content") ? paramsNode.get("content").asText() : "";

                        CreateFileRequest fileReq = new CreateFileRequest(p, c);
                        createFileTool.createFile(fileReq);
                        return "I have created the file '" + p + "' successfully.";
                    }
                }
            } catch (Exception e) {
                System.err.println("Simulated tool call extraction fallback error: " + e.getMessage());
            }
        }
        return responseText;
    }
}