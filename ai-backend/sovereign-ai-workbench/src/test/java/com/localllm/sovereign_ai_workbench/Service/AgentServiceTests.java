package com.localllm.sovereign_ai_workbench.Service;

import com.localllm.sovereign_ai_workbench.Tools.CreateDocumentRequest;
import com.localllm.sovereign_ai_workbench.Tools.CreateDocumentTool;
import com.localllm.sovereign_ai_workbench.Tools.KnowledgeSearchTool;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
