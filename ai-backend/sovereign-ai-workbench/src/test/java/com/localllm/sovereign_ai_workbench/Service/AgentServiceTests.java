package com.localllm.sovereign_ai_workbench.Service;

import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionRequest;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionResult;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionTool;
import com.localllm.sovereign_ai_workbench.Tools.CreateDocumentRequest;
import com.localllm.sovereign_ai_workbench.Tools.CreateDocumentTool;
import com.localllm.sovereign_ai_workbench.Tools.CreateFileRequest;
import com.localllm.sovereign_ai_workbench.Tools.CreateFileTool;
import com.localllm.sovereign_ai_workbench.Tools.KnowledgeSearchTool;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceTests {

    @Test
    void recoversDocumentToolCallEmbeddedInProseWithRequestWrapper() {
        CreateDocumentTool documentTool = mock(CreateDocumentTool.class);
        when(documentTool.createFormattedDocument(any(CreateDocumentRequest.class)))
                .thenReturn("Document 'loto_approval_note.docx' generated successfully.");

        AgentService service = new AgentService(
                null, null, null, null, null, null, null, null,
                documentTool, null, null,
                "ollama", "http://localhost:11434", "http://localhost:1234"
        );

        String modelResponse = """
                Here is the JSON for a function call with its proper arguments:

                {"name":"create_formatted_document","parameters":{"request":{"title":"LOTO Approval Note","content":"# Background\\nLockout/tagout is required.","path":"loto_approval_note.docx"}}
                """;

        String result = service.handleTextSimulatedToolCalls("test-conversation", modelResponse);

        ArgumentCaptor<CreateDocumentRequest> requestCaptor = ArgumentCaptor.forClass(CreateDocumentRequest.class);
        verify(documentTool).createFormattedDocument(requestCaptor.capture());
        assertEquals("loto_approval_note.docx", requestCaptor.getValue().getPath());
        assertEquals("LOTO Approval Note", requestCaptor.getValue().getTitle());
        assertEquals("# Background\nLockout/tagout is required.", requestCaptor.getValue().getContent());
        assertEquals("Document 'loto_approval_note.docx' generated successfully.", result);
    }

    @Test
    void prefetchesLocalEvidenceForUploadedManualDeliverables() {
        KnowledgeSearchTool knowledgeTool = mock(KnowledgeSearchTool.class);
        when(knowledgeTool.searchKnowledgeBase(any(KnowledgeSearchTool.KnowledgeSearchRequest.class)))
                .thenReturn("Local knowledge base results:\nSource: osha-lockout-tagout.pdf\nLOTO evidence");

        AgentService service = new AgentService(
                null, null, null, null, null, null, null, null,
                null, knowledgeTool, null,
                "ollama", "http://localhost:11434", "http://localhost:1234"
        );

        String prompt = service.enrichKnowledgeBackedDeliverable(
                "Using the uploaded OSHA manual, create loto_approval_note.docx"
        );

        assertTrue(prompt.contains("osha-lockout-tagout.pdf"));
        assertTrue(prompt.contains("retrieved on-premise evidence"));
        verify(knowledgeTool).searchKnowledgeBase(any(KnowledgeSearchTool.KnowledgeSearchRequest.class));
    }

    @Test
    void routesNonDocumentExtensionAwayFromFormattedDocumentTool() {
        CreateDocumentTool documentTool = mock(CreateDocumentTool.class);
        CreateFileTool fileTool = mock(CreateFileTool.class);
        when(fileTool.createFile(any(CreateFileRequest.class)))
                .thenReturn("File 'loto_risk_register.csv' created successfully.");

        AgentService service = new AgentService(
                null, null, null, null, fileTool, null, null, null,
                documentTool, null, null,
                "ollama", "http://localhost:11434", "http://localhost:1234"
        );

        String response = """
                {
                  "name": "create_formatted_document",
                  "arguments": {
                    "path": "loto_risk_register.csv",
                    "title": "LOTO Risk Register",
                    "content": "activity,hazardous_energy,risk_level,loto_required\\nPump,Electrical,High,Yes"
                  }
                }
                """;

        String result = service.handleTextSimulatedToolCalls("test-conversation", response);

        ArgumentCaptor<CreateFileRequest> requestCaptor = ArgumentCaptor.forClass(CreateFileRequest.class);
        verify(fileTool).createFile(requestCaptor.capture());
        assertEquals("loto_risk_register.csv", requestCaptor.getValue().getPath());
        assertTrue(requestCaptor.getValue().getContent().startsWith("activity,"));
        assertEquals("File 'loto_risk_register.csv' created successfully.", result);
        verifyNoInteractions(documentTool);
    }

    @Test
    void executesPythonInsteadOfOnlySavingItWhenExecutionWasRequested() {
        CodeExecutionTool codeTool = mock(CodeExecutionTool.class);
        CreateFileTool fileTool = mock(CreateFileTool.class);
        when(codeTool.executePythonCode(any(CodeExecutionRequest.class)))
                .thenReturn(new CodeExecutionResult(0, "Number of high-risk activities: 3\n", false,
                        List.of("output/loto_risk_register.csv")));

        AgentService service = new AgentService(
                null, null, null, codeTool, fileTool, null, null, null,
                null, null, null,
                "ollama", "http://localhost:11434", "http://localhost:1234"
        );

        String response = """
                {"name":"create_file","arguments":{"path":"loto_risk_register.py","content":"import pandas as pd\\nprint('done')"}}
                """;
        String result = service.handleTextSimulatedToolCalls(
                "test-conversation",
                response,
                "Create and execute a Python script in the sandbox."
        );

        ArgumentCaptor<CodeExecutionRequest> requestCaptor = ArgumentCaptor.forClass(CodeExecutionRequest.class);
        verify(codeTool).executePythonCode(requestCaptor.capture());
        assertEquals("loto_risk_register.py", requestCaptor.getValue().getEntryFile());
        assertTrue(requestCaptor.getValue().getFiles().containsKey("loto_risk_register.py"));
        assertTrue(result.contains("exit code 0"));
        assertTrue(result.contains("high-risk activities: 3"));
        verifyNoInteractions(fileTool);
    }

    @Test
    void replacesPersistedToolJsonWithReadableHistory() {
        ChatMemory chatMemory = mock(ChatMemory.class);
        String rawToolJson = """
                ```json
                {"name":"create_file","arguments":{"path":"loto_risk_register.py","content":"print('done')"}}
                ```
                """;
        when(chatMemory.get("history-test")).thenReturn(List.of(
                new UserMessage("Create a script\n\nMANDATORY EXECUTION REQUIREMENT:\nInternal routing instructions"),
                new AssistantMessage(rawToolJson)
        ));

        AgentService service = new AgentService(
                null, chatMemory, null, null, null, null, null, null,
                null, null, null,
                "ollama", "http://localhost:11434", "http://localhost:1234"
        );

        List<Message> history = service.getChatHistory("history-test");

        assertEquals(2, history.size());
        assertEquals("Create a script", history.get(0).getText());
        assertTrue(history.get(1).getText().contains("loto_risk_register.py"));
        assertFalse(history.get(1).getText().contains("\"arguments\""));
        verify(chatMemory).clear("history-test");
        verify(chatMemory).add(eq("history-test"), anyList());
    }

    @Test
    void recoversPythonWhenModelLeavesInnerJsonQuotesUnescaped() {
        CodeExecutionTool codeTool = mock(CodeExecutionTool.class);
        when(codeTool.executePythonCode(any(CodeExecutionRequest.class)))
                .thenReturn(new CodeExecutionResult(0, "SANDBOX_TEST_PASSED\n", false,
                        List.of("output/demo_verification.json")));

        AgentService service = new AgentService(
                null, null, null, codeTool, null, null, null, null,
                null, null, null,
                "ollama", "http://localhost:11434", "http://localhost:1234"
        );

        String malformed = """
                {"name":"execute_python_code","parameters":{"language":"python","files":{"main.py":"import json\\npayload = {"total": 100, "verified": True}\\nprint("SANDBOX_TEST_PASSED")"},"entryFile":"main.py"}}
                """;

        String result = service.handleTextSimulatedToolCalls(
                "test-conversation", malformed, "Create and execute a Python script in the sandbox."
        );

        ArgumentCaptor<CodeExecutionRequest> requestCaptor = ArgumentCaptor.forClass(CodeExecutionRequest.class);
        verify(codeTool).executePythonCode(requestCaptor.capture());
        assertTrue(requestCaptor.getValue().getFiles().get("main.py").contains("\"total\": 100"));
        assertTrue(result.contains("SANDBOX_TEST_PASSED"));
    }

    @Test
    void recoversMalformedCreateFileAsExecutionWhenRunWasRequested() {
        CodeExecutionTool codeTool = mock(CodeExecutionTool.class);
        when(codeTool.executePythonCode(any(CodeExecutionRequest.class)))
                .thenReturn(new CodeExecutionResult(0, "SANDBOX_TEST_PASSED\n", false, List.of()));

        AgentService service = new AgentService(
                null, null, null, codeTool, null, null, null, null,
                null, null, null,
                "ollama", "http://localhost:11434", "http://localhost:1234"
        );

        String malformed = """
                {"name":"create_file","arguments":{"path":"verify.py","content":"payload = {"total": 100}\\nprint("SANDBOX_TEST_PASSED")"}}
                """;
        String result = service.handleTextSimulatedToolCalls(
                "test-conversation", malformed, "Run and verify this Python script in Docker."
        );

        ArgumentCaptor<CodeExecutionRequest> requestCaptor = ArgumentCaptor.forClass(CodeExecutionRequest.class);
        verify(codeTool).executePythonCode(requestCaptor.capture());
        assertEquals("verify.py", requestCaptor.getValue().getEntryFile());
        assertTrue(result.contains("exit code 0"));
    }
}
