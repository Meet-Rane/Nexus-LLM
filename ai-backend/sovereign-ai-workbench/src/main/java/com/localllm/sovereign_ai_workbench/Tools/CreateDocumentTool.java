package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Dto.ArtifactDto;
import com.localllm.sovereign_ai_workbench.Entity.Artifact;
import com.localllm.sovereign_ai_workbench.Service.ArtifactService;
import com.localllm.sovereign_ai_workbench.Service.DocumentGenerationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CreateDocumentTool {

    private final DocumentGenerationService documentGenerationService;
    private final ArtifactService artifactService;

    public CreateDocumentTool(
            DocumentGenerationService documentGenerationService,
            ArtifactService artifactService) {
        this.documentGenerationService = documentGenerationService;
        this.artifactService = artifactService;
    }

    @Tool(
        name = "create_formatted_document",
        description = """
            Create a professionally formatted native PDF, Word, Excel, or PowerPoint deliverable from structured text.

            Use this tool WHENEVER the user asks to generate or export PDF, Word (.docx), Excel (.xlsx), or PowerPoint (.pptx) output.

            Parameters:
            - path: filename ending in '.pdf', '.docx', '.xlsx', or '.pptx'
            - title: executive title of the document (e.g. 'MRPL Reboiler Maintenance & Overhaul Guide')
            - content: complete content using markdown headings and bullets. For Excel, provide a markdown table or CSV rows. For PowerPoint, use one ## heading per slide followed by concise bullet lines.

            Advantages of this tool:
            - Native editable Word paragraphs, Excel cells, and PowerPoint slide text
            - Professional industrial styling and safe local artifact storage
            - No Python or cloud conversion service is required
            """
    )
    public String createFormattedDocument(CreateDocumentRequest request) {
        String conversationId = ConversationContextHolder.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "Error: Active conversation context not found.";
        }

        String path = request.getPath() != null ? request.getPath().trim() : "document.pdf";
        String content = request.getContent() != null ? request.getContent() : "";
        String title = request.getTitle();

        // Extract title from first markdown header if title was omitted
        if ((title == null || title.isBlank() || title.equalsIgnoreCase("Document")) && !content.isBlank()) {
            for (String line : content.split("\r?\n")) {
                String t = line.trim();
                if (t.startsWith("#")) {
                    title = t.replaceAll("^#+\\s*", "").trim();
                    break;
                }
            }
        }

        // If path was defaulted to generic document.pdf, derive a descriptive filename from title
        if ((path.equalsIgnoreCase("document.pdf") || path.equalsIgnoreCase("document.docx")) && title != null && !title.isBlank() && !title.equalsIgnoreCase("Document")) {
            String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
            if (!slug.isBlank()) {
                path = slug + (path.endsWith(".docx") ? ".docx" : ".pdf");
            }
        }

        if (title == null || title.isBlank()) {
            title = "MRPL Technical Deliverable";
        }

        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".doc")) {
            path = path.substring(0, path.length() - 4) + ".docx";
            lowerPath = path.toLowerCase();
        }
        String cleanFileName = path.replaceAll("^[\\\\/]+", "").replace('\\', '/');
        if (cleanFileName.contains("/")) {
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf('/') + 1);
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart("create_formatted_document", "Generating document: " + cleanFileName));

        try {
            byte[] bytes;
            if (lowerPath.endsWith(".docx")) {
                bytes = documentGenerationService.generateDocx(title, content);
            } else if (lowerPath.endsWith(".xlsx")) {
                bytes = documentGenerationService.generateXlsx(title, content);
            } else if (lowerPath.endsWith(".pptx")) {
                bytes = documentGenerationService.generatePptx(title, content);
            } else {
                if (!lowerPath.endsWith(".pdf")) {
                    path = path + ".pdf";
                }
                bytes = documentGenerationService.generatePdf(title, content);
            }

            Artifact artifact = artifactService.saveFileBytes(conversationId, path, bytes);
            ArtifactDto dto = ArtifactDto.fromEntity(artifact);

            String resultMsg = "Document '" + artifact.getFileName() + "' generated successfully (" + artifact.getFileSize() + " bytes).";

            ConversationContextHolder.emitEvent(AgentStreamEvent.artifactCreated(dto));
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolCompleteWithArtifact("create_formatted_document", resultMsg, dto));

            return resultMsg;
        } catch (Exception e) {
            String errorMsg = "Failed to generate document: " + e.getMessage();
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("create_formatted_document", errorMsg));
            return errorMsg;
        }
    }
}
