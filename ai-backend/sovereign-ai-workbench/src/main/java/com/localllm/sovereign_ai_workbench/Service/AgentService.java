package com.localllm.sovereign_ai_workbench.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

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
import com.localllm.sovereign_ai_workbench.Tools.KnowledgeSearchTool;
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
               - FOR A DOCUMENT BASED ON AN UPLOADED MANUAL, SOP, OR REPORT: Ground it in the supplied local knowledge-base results and preserve the real source filename in the document.
               - FOR COMPUTATIONAL SIMULATIONS & CODE EXECUTION: Use 'execute_python_code' (pass code in 'files' map and set 'entryFile').
               - If the user says run, execute, verify, or test Python/code, NEVER stop after 'create_file'. Invoke 'execute_python_code' so the code actually runs in the Docker sandbox.
               - FOR SAVING DATA & SOURCE CODE FILES (.py, .csv, .json, .sql, .txt): Use 'create_file'.
               - FOR QUESTIONS ABOUT INTERNAL MANUALS, SOPS, OR UPLOADED REPORTS: Call 'search_knowledge_base' before answering and cite the returned source filenames.
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
    private final KnowledgeSearchTool knowledgeSearchTool;
    private final NetworkAuditService networkAuditService;
    private final String provider;
    private final String ollamaBaseUrl;
    private final String externalAiBaseUrl;

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
            KnowledgeSearchTool knowledgeSearchTool,
            NetworkAuditService networkAuditService,
            @Value("${ai.provider}") String provider,
            @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl,
            @Value("${spring.ai.openai.base-url}") String externalAiBaseUrl
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
        this.knowledgeSearchTool = knowledgeSearchTool;
        this.networkAuditService = networkAuditService;
        this.provider = provider;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.externalAiBaseUrl = externalAiBaseUrl;
    }

    public List<Message> getChatHistory(String conversationId) {
        return normalizeConversationHistory(conversationId);
    }

    public String chat(String conversationId, String message) {
        try {
            normalizeConversationHistory(conversationId);
            ConversationContextHolder.setConversationId(conversationId);

            RouteDecision decision = modelRouter.selectModel(conversationId, message);
            String selectedModel = decision.model();
            recordModelRequest(selectedModel);
            String effectiveMessage = enforceExecutionTool(message, enrichKnowledgeBackedDeliverable(message));

            System.out.println("Provider: " + provider);
            System.out.println("Selected model: " + selectedModel + " (" + decision.category() + ")");

            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(codeExecutionTool, createFileTool, readFileTool, writeFileTool, listFilesTool, createDocumentTool, knowledgeSearchTool)
                    .user(effectiveMessage);

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

            String rawResponseText = request.call().content();
            String responseText = handleTextSimulatedToolCalls(conversationId, rawResponseText, message);
            responseText = ensureKnowledgeCitations(responseText);
            replaceLastAssistantResponse(conversationId, rawResponseText, responseText);
            return responseText;
        } finally {
            ConversationContextHolder.clear();
        }
    }

    public Flux<AgentStreamEvent> streamChat(String conversationId, String message) {
        return Flux.<AgentStreamEvent>create(sink -> {
            try {
                normalizeConversationHistory(conversationId);
                ConversationContextHolder.setConversationId(conversationId);
                ConversationContextHolder.setEventListener(sink::next);

                RouteDecision decision = modelRouter.selectModel(conversationId, message);
                String selectedModel = decision.model();
                recordModelRequest(selectedModel);

                // Announce router decision with full reasoning to the UI stream
                sink.next(AgentStreamEvent.router(selectedModel, decision.reason()));
                String effectiveMessage = enforceExecutionTool(message, enrichKnowledgeBackedDeliverable(message));

                ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .tools(codeExecutionTool, createFileTool, readFileTool, writeFileTool, listFilesTool, createDocumentTool, knowledgeSearchTool)
                        .user(effectiveMessage);

                if ("ollama".equalsIgnoreCase(provider)) {
                    request.options(OllamaChatOptions.builder().model(selectedModel));
                } else if ("nvidia".equalsIgnoreCase(provider)) {
                    request.options(OpenAiChatOptions.builder().model(selectedModel));
                } else {
                    throw new IllegalArgumentException("Unsupported AI provider: " + provider);
                }

                // Execute agent request reliably
                String rawResponseText = request.call().content();

                // Intercept simulated markdown tool calls generated by open-weight models
                String responseText = handleTextSimulatedToolCalls(conversationId, rawResponseText, message);
                responseText = ensureKnowledgeCitations(responseText);
                replaceLastAssistantResponse(conversationId, rawResponseText, responseText);

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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Auto-recovery interceptor for smaller open-weight models that occasionally output
     * markdown JSON tool call blocks in their text response instead of native tool tokens.
     */
    String handleTextSimulatedToolCalls(String conversationId, String responseText) {
        return handleTextSimulatedToolCalls(conversationId, responseText, null);
    }

    String handleTextSimulatedToolCalls(String conversationId, String responseText, String userMessage) {
        if (responseText == null || responseText.isBlank()) {
            return responseText;
        }

        ParsedToolCall toolCall = parseSimulatedToolCall(responseText);
        if (toolCall == null) {
            if (looksLikeToolPayload(responseText)) {
                return "The local model produced an invalid tool request, so it was not shown as raw JSON. Please retry the task.";
            }
            return responseText;
        }

        try {
            String toolName = toolCall.name();
            JsonNode paramsNode = toolCall.parameters();
            ObjectMapper mapper = configuredObjectMapper();

            if (toolName.contains("search_knowledge_base")) {
                String query = paramsNode.has("query") ? paramsNode.get("query").asText() : "";
                Integer topK = paramsNode.has("topK") ? paramsNode.get("topK").asInt() : 5;
                return knowledgeSearchTool.searchKnowledgeBase(
                        new KnowledgeSearchTool.KnowledgeSearchRequest(query, topK)
                );
            } else if (toolName.contains("execute_python_code")) {
                CodeExecutionRequest codeRequest = mapper.treeToValue(paramsNode, CodeExecutionRequest.class);
                return executePythonAndSummarize(codeRequest);
            } else if (toolName.contains("read_file")) {
                String p = paramsNode.has("path") ? paramsNode.get("path").asText() : "file.txt";
                return readFileTool.readFile(new ReadFileRequest(p));
            } else if (toolName.contains("write_file")) {
                String p = paramsNode.has("path") ? paramsNode.get("path").asText() : "file.txt";
                String c = paramsNode.has("content") ? paramsNode.get("content").asText() : "";
                return writeFileTool.writeFile(new WriteFileRequest(p, c));
            } else if (toolName.contains("list_files")) {
                return listFilesTool.listFiles(new ListFilesTool.ListFilesRequest());
            } else if (toolName.contains("create_formatted_document")) {
                String p = paramsNode.has("path") ? paramsNode.get("path").asText() : "document.pdf";
                String t = paramsNode.has("title") ? paramsNode.get("title").asText() : "MRPL Deliverable";
                String c = paramsNode.has("content") ? paramsNode.get("content").asText() : "";

                if (!c.isBlank()) {
                    if (!isFormattedDocumentPath(p)) {
                        return createFileTool.createFile(new CreateFileRequest(p, c));
                    }
                    return createDocumentTool.createFormattedDocument(new CreateDocumentRequest(p, t, c));
                }
            } else if (toolName.contains("create_file")
                    || (toolName.isBlank() && paramsNode.has("path") && paramsNode.has("content"))) {
                String p = paramsNode.has("path") ? paramsNode.get("path").asText() : "file.txt";
                String c = paramsNode.has("content") ? paramsNode.get("content").asText() : "";

                if (requiresPythonExecution(userMessage) && looksLikePythonSource(p, c)) {
                    String entryFile = simpleEntryFile(p);
                    return executePythonAndSummarize(
                            new CodeExecutionRequest("python", Map.of(entryFile, c), entryFile)
                    );
                }
                return createFileTool.createFile(new CreateFileRequest(p, c));
            }
        } catch (Exception e) {
            System.err.println("Simulated tool call extraction fallback error: " + e.getMessage());
            return "The local tool request failed: " + (e.getMessage() != null ? e.getMessage() : "unknown execution error");
        }

        return "The local model selected a tool but did not provide the required arguments. Please retry the task.";
    }

    private String executePythonAndSummarize(CodeExecutionRequest codeRequest) {
        var result = codeExecutionTool.executePythonCode(codeRequest);
        return "Execution finished with exit code " + result.getExitCode()
                + ".\n\n" + result.getOutput();
    }

    private ParsedToolCall parseSimulatedToolCall(String responseText) {
        if (!looksLikeToolPayload(responseText)) {
            return null;
        }

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

            String trimmedCandidate = jsonCandidate.trim();
            if (!trimmedCandidate.startsWith("{")) {
                int objectStart = trimmedCandidate.indexOf('{');
                int objectEnd = trimmedCandidate.lastIndexOf('}');
                if (objectStart >= 0 && objectEnd > objectStart) {
                    trimmedCandidate = trimmedCandidate.substring(objectStart, objectEnd + 1);
                }
            }
            trimmedCandidate = closeTruncatedJsonObject(trimmedCandidate);
            if (!trimmedCandidate.startsWith("{")) {
                return null;
            }

            ObjectMapper mapper = configuredObjectMapper();
            JsonNode root = mapper.readTree(trimmedCandidate);
            if (root.has("function") && root.get("function").isObject()) {
                root = root.get("function");
            }

            String toolName = root.has("name") ? root.get("name").asText() : "";
            JsonNode paramsNode = root.has("parameters")
                    ? root.get("parameters")
                    : root.has("arguments") ? root.get("arguments") : root;
            if (paramsNode.isTextual() && paramsNode.asText().trim().startsWith("{")) {
                paramsNode = mapper.readTree(paramsNode.asText());
            }
            if (paramsNode.has("request") && paramsNode.get("request").isObject()) {
                paramsNode = paramsNode.get("request");
            }
            return new ParsedToolCall(toolName, paramsNode);
        } catch (Exception e) {
            System.err.println("Simulated tool call parsing error: " + e.getMessage());
            return null;
        }
    }

    private static ObjectMapper configuredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
        return mapper;
    }

    private static boolean looksLikeToolPayload(String responseText) {
        if (responseText == null) {
            return false;
        }
        String lower = responseText.toLowerCase();
        boolean knownTool = lower.contains("create_formatted_document")
                || lower.contains("create_file")
                || lower.contains("write_file")
                || lower.contains("read_file")
                || lower.contains("list_files")
                || lower.contains("search_knowledge_base")
                || lower.contains("execute_python_code");
        return knownTool && (lower.contains("\"name\"")
                || lower.contains("\"function\"")
                || lower.trim().startsWith("```")
                || lower.trim().startsWith("{"));
    }

    private static boolean isFormattedDocumentPath(String path) {
        String lower = path == null ? "" : path.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx");
    }

    private static boolean looksLikePythonSource(String path, String content) {
        String lowerPath = path == null ? "" : path.toLowerCase();
        String lowerContent = content == null ? "" : content.toLowerCase();
        return lowerPath.endsWith(".py")
                || lowerContent.contains("import ")
                || lowerContent.contains("from ")
                || lowerContent.contains("def ")
                || lowerContent.contains("print(");
    }

    private static String simpleEntryFile(String path) {
        String normalized = path == null || path.isBlank() ? "main.py" : path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return fileName.toLowerCase().endsWith(".py") ? fileName : "main.py";
    }

    private record ParsedToolCall(String name, JsonNode parameters) {
    }

    private static String enforceExecutionTool(String originalMessage, String effectiveMessage) {
        if (!requiresPythonExecution(originalMessage)) {
            return effectiveMessage;
        }
        return effectiveMessage + """


                MANDATORY EXECUTION REQUIREMENT:
                The user explicitly requested that code be run and verified. Invoke execute_python_code now.
                Put the complete Python program in the files map, set entryFile to that .py filename,
                save generated deliverables under output/, and do not use create_file or create_formatted_document instead.
                """;
    }

    private static boolean requiresPythonExecution(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        boolean executionVerb = lower.contains("execute")
                || lower.contains("run ")
                || lower.contains("run the")
                || lower.contains("verify")
                || lower.contains("test ")
                || lower.contains("sandbox");
        boolean codeSubject = lower.contains("python")
                || lower.contains("script")
                || lower.contains("code")
                || lower.contains("program");
        return executionVerb && codeSubject;
    }

    private synchronized List<Message> normalizeConversationHistory(String conversationId) {
        List<Message> original = chatMemory.get(conversationId);
        if (original == null || original.isEmpty()) {
            return original == null ? List.of() : original;
        }

        List<Message> normalized = new ArrayList<>(original.size());
        boolean changed = false;
        for (Message message : original) {
            if (message.getMessageType() == MessageType.ASSISTANT && looksLikeToolPayload(message.getText())) {
                String cleanText = summarizeToolCallForHistory(message.getText());
                normalized.add(new AssistantMessage(cleanText));
                changed = true;
            } else if (message.getMessageType() == MessageType.USER) {
                String cleanText = stripInjectedPromptContext(message.getText());
                if (!Objects.equals(cleanText, message.getText())) {
                    normalized.add(new org.springframework.ai.chat.messages.UserMessage(cleanText));
                    changed = true;
                } else {
                    normalized.add(message);
                }
            } else {
                normalized.add(message);
            }
        }

        if (changed) {
            chatMemory.clear(conversationId);
            chatMemory.add(conversationId, normalized);
        }
        return normalized;
    }

    private static String stripInjectedPromptContext(String text) {
        if (text == null) {
            return null;
        }
        int cutoff = text.length();
        for (String marker : List.of(
                "\n\nMANDATORY EXECUTION REQUIREMENT:",
                "\n\nUse the following retrieved on-premise evidence as the factual basis for the document.")) {
            int markerIndex = text.indexOf(marker);
            if (markerIndex >= 0) {
                cutoff = Math.min(cutoff, markerIndex);
            }
        }
        return text.substring(0, cutoff).stripTrailing();
    }

    private synchronized void replaceLastAssistantResponse(
            String conversationId,
            String rawResponse,
            String processedResponse) {
        if (Objects.equals(rawResponse, processedResponse) || processedResponse == null || processedResponse.isBlank()) {
            return;
        }

        List<Message> messages = new ArrayList<>(chatMemory.get(conversationId));
        for (int index = messages.size() - 1; index >= 0; index--) {
            Message candidate = messages.get(index);
            if (candidate.getMessageType() == MessageType.ASSISTANT
                    && Objects.equals(candidate.getText(), rawResponse)) {
                messages.set(index, new AssistantMessage(processedResponse));
                chatMemory.clear(conversationId);
                chatMemory.add(conversationId, messages);
                return;
            }
        }
    }

    String summarizeToolCallForHistory(String responseText) {
        ParsedToolCall toolCall = parseSimulatedToolCall(responseText);
        if (toolCall == null) {
            return "The local agent prepared a tool action, but its saved result was unavailable. Please run the task again.";
        }

        JsonNode params = toolCall.parameters();
        String path = params.has("path") ? params.get("path").asText() : "";
        String simplePath = simpleFileName(path);
        String toolName = toolCall.name();
        if (toolName.contains("execute_python_code")) {
            return "Python code was executed in the local Docker sandbox. Open Artifacts to inspect generated files.";
        }
        if (toolName.contains("create_formatted_document")) {
            return simplePath.isBlank()
                    ? "A formatted document was generated locally. Open Artifacts to download it."
                    : "Generated '" + simplePath + "' locally. Open Artifacts to download it.";
        }
        if (toolName.contains("create_file") || toolName.contains("write_file")) {
            return simplePath.isBlank()
                    ? "A file was saved locally. Open Artifacts to inspect it."
                    : "Saved '" + simplePath + "' locally. Open Artifacts to inspect it.";
        }
        if (toolName.contains("read_file")) {
            return simplePath.isBlank() ? "Read a local artifact." : "Read local artifact '" + simplePath + "'.";
        }
        if (toolName.contains("list_files")) {
            return "Listed files from the local artifact store.";
        }
        if (toolName.contains("search_knowledge_base")) {
            return "Searched the local knowledge base.";
        }
        return "The local agent completed a tool action.";
    }

    private static String simpleFileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private static String closeTruncatedJsonObject(String candidate) {
        int objectDepth = 0;
        boolean insideString = false;
        boolean escaped = false;

        for (int i = 0; i < candidate.length(); i++) {
            char current = candidate.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && insideString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                insideString = !insideString;
                continue;
            }
            if (!insideString) {
                if (current == '{') {
                    objectDepth++;
                } else if (current == '}') {
                    objectDepth--;
                }
            }
        }

        if (insideString || objectDepth <= 0) {
            return candidate;
        }
        return candidate + "}".repeat(objectDepth);
    }

    String enrichKnowledgeBackedDeliverable(String message) {
        String lowerMessage = message == null ? "" : message.toLowerCase();
        boolean referencesLocalKnowledge = lowerMessage.contains("uploaded")
                || lowerMessage.contains("manual")
                || lowerMessage.contains("sop")
                || lowerMessage.contains("inspection report");
        boolean requestsDocument = lowerMessage.contains("approval note")
                || lowerMessage.contains("word")
                || lowerMessage.contains(".docx")
                || lowerMessage.contains("pdf")
                || lowerMessage.contains("document");

        if (!referencesLocalKnowledge || !requestsDocument) {
            return message;
        }

        String knowledge = knowledgeSearchTool.searchKnowledgeBase(
                new KnowledgeSearchTool.KnowledgeSearchRequest(message, 3)
        );
        if (knowledge.startsWith("Knowledge base search failed:")
                || knowledge.startsWith("No relevant passages")) {
            return message;
        }

        return message + """


                Use the following retrieved on-premise evidence as the factual basis for the document.
                Preserve its real source filename in the Source section:

                """ + knowledge;
    }

    private void recordModelRequest(String selectedModel) {
        String endpoint = "ollama".equalsIgnoreCase(provider) ? ollamaBaseUrl : externalAiBaseUrl;
        networkAuditService.record("MODEL", endpoint, "chat · " + selectedModel);
    }

    private String ensureKnowledgeCitations(String responseText) {
        List<String> missingSources = ConversationContextHolder.getKnowledgeSources().stream()
                .filter(source -> responseText == null || !responseText.toLowerCase().contains(source.toLowerCase()))
                .toList();
        if (missingSources.isEmpty()) {
            return responseText;
        }

        String citations = String.join(", ", missingSources);
        if (responseText == null || responseText.isBlank()) {
            return "Source: " + citations;
        }
        return responseText.stripTrailing() + "\n\nSource: " + citations;
    }
}
