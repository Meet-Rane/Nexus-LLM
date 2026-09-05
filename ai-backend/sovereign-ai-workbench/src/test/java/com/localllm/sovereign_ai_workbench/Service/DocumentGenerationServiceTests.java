package com.localllm.sovereign_ai_workbench.Service;

import com.lowagie.text.pdf.PdfReader;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentGenerationServiceTests {

    private final DocumentGenerationService service = new DocumentGenerationService();

    @Test
    void generatesReadablePdf() throws Exception {
        byte[] bytes = service.generatePdf(
                "Pump Inspection Report",
                "# Findings\n- Seal leakage observed\n- **Action:** replace gasket"
        );

        assertTrue(bytes.length > 1_000);
        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));

        PdfReader reader = new PdfReader(bytes);
        assertEquals(1, reader.getNumberOfPages());
        reader.close();
    }

    @Test
    void generatesReadableDocx() throws Exception {
        byte[] bytes = service.generateDocx(
                "Pump Approval Note",
                "# Recommendation\nApprove replacement of the **mechanical seal**."
        );

        assertTrue(bytes.length > 1_000);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + "\n" + right);
            assertTrue(text.contains("Pump Approval Note"));
            assertTrue(text.contains("mechanical seal"));
        }
    }
}
