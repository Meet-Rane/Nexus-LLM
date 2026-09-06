package com.localllm.sovereign_ai_workbench.Service;

import com.lowagie.text.pdf.PdfReader;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    @Test
    void generatesNativeExcelWorkbookWithEditableCells() throws Exception {
        byte[] bytes = service.generateXlsx(
                "LOTO Risk Register",
                "| Activity | Hazardous energy | Risk level | LOTO required |\n"
                        + "|---|---|---|---|\n"
                        + "| Pump maintenance | Electrical | High | Yes |\n"
                        + "| Visual inspection | None | Low | No |"
        );

        assertTrue(bytes.length > 1_000);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals("LOTO Risk Register", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Activity", workbook.getSheetAt(0).getRow(3).getCell(0).getStringCellValue());
            assertEquals("Pump maintenance", workbook.getSheetAt(0).getRow(4).getCell(0).getStringCellValue());
            assertTrue(workbook.getSheetAt(0).getPaneInformation().isFreezePane());
        }
    }

    @Test
    void generatesNativePowerPointWithEditableSlideText() throws Exception {
        byte[] bytes = service.generatePptx(
                "Turnaround Safety Briefing",
                "## Scope\n- Isolate process equipment\n- Verify zero energy\n"
                        + "## Responsibilities\n- Supervisor signs the permit\n- Operator applies the lock"
        );

        assertTrue(bytes.length > 1_000);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);

        try (XMLSlideShow presentation = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
            assertEquals(3, presentation.getSlides().size());
            String allText = presentation.getSlides().stream()
                    .flatMap(slide -> slide.getShapes().stream())
                    .filter(shape -> shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape)
                    .map(shape -> ((org.apache.poi.xslf.usermodel.XSLFTextShape) shape).getText())
                    .reduce("", (left, right) -> left + "\n" + right);
            assertTrue(allText.contains("Turnaround Safety Briefing"));
            assertTrue(allText.contains("Verify zero energy"));
            assertTrue(allText.contains("Supervisor signs the permit"));
        }
    }
}
