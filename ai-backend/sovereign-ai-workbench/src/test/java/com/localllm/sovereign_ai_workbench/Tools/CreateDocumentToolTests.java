package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import com.localllm.sovereign_ai_workbench.Service.DocumentGenerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateDocumentToolTests {

    @AfterEach
    void clearContext() {
        ConversationContextHolder.clear();
    }

    @Test
    void stripsDirectoriesFromModelGeneratedDocumentPath() throws Exception {
        DocumentGenerationService generationService = mock(DocumentGenerationService.class);
        ArtifactService artifactService = mock(ArtifactService.class);
        CreateDocumentTool tool = new CreateDocumentTool(generationService, artifactService);
        byte[] content = new byte[]{1, 2, 3};
        Artifact saved = new Artifact();
        saved.setFileName("refinery_summary_2022_2025.docx");
        saved.setFilePath("refinery_summary_2022_2025.docx");
        saved.setFileSize(content.length);

        when(generationService.generateDocx(any(), any())).thenReturn(content);
        when(artifactService.saveFileBytes(eq("test-conversation"), any(), eq(content))).thenReturn(saved);
        ConversationContextHolder.setConversationId("test-conversation");

        String result = tool.createFormattedDocument(new CreateDocumentRequest(
                "C:\\Users\\Documents\\refinery_summary_2022_2025.docx",
                "Refinery Summary",
                "# Summary\nVerified local content"));

        ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(artifactService).saveFileBytes(eq("test-conversation"), path.capture(), eq(content));
        assertThat(path.getValue()).isEqualTo("refinery_summary_2022_2025.docx");
        assertThat(result).contains("generated successfully");
    }
}
