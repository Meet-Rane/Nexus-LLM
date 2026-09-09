package com.localllm.sovereign_ai_workbench.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentGenerationService {

    private static final Color NAVY_BLUE = new Color(26, 54, 93);
    private static final Color SLATE_BLUE = new Color(43, 108, 176);
    private static final Color DARK_GRAY = new Color(45, 55, 72);
    private static final Color MUTED_GRAY = new Color(113, 128, 150);

    private static final Pattern INLINE_PATTERN = Pattern.compile("(\\*\\*([^*]+)\\*\\*)|(`([^`]+)`)|(\\*([^*]+)\\*)");

    public byte[] generatePdf(String title, String content) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // Header / Footer Page Event
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, com.lowagie.text.Document document) {
                    PdfContentByte cb = writer.getDirectContent();
                    ColumnText.showTextAligned(
                        cb, Element.ALIGN_RIGHT,
                        new Phrase("Page " + writer.getPageNumber(), FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED_GRAY)),
                        document.right(), document.bottom() - 20, 0
                    );
                }
            });

            document.open();

            // 1. Executive Title Banner
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(NAVY_BLUE);
            cell.setPadding(14);
            cell.setBorder(Rectangle.NO_BORDER);

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
            Paragraph titlePara = new Paragraph(title != null && !title.isBlank() ? title : "MRPL Technical Deliverable", titleFont);
            titlePara.setAlignment(Element.ALIGN_LEFT);
            cell.addElement(titlePara);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(226, 232, 240));
            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Paragraph datePara = new Paragraph("Sovereign AI Workbench \u2022 Mangalore Refinery & Petrochemicals Ltd \u2022 " + timeStr, dateFont);
            datePara.setSpacingBefore(4);
            cell.addElement(datePara);

            headerTable.addCell(cell);
            document.add(headerTable);
            document.add(new Paragraph(" "));

            // 2. Parse Markdown Content Lines
            if (content != null && !content.isBlank()) {
                String[] lines = content.split("\r?\n");

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    String trimmed = line.trim();

                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    // Ignore underline markers (=== or ---)
                    if (trimmed.matches("^[=\\-]{3,}$")) {
                        continue;
                    }

                    // Headings (# Heading 1, ## Heading 2, ### Heading 3)
                    if (trimmed.startsWith("#")) {
                        int level = 0;
                        while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                            level++;
                        }
                        String headingText = trimmed.substring(level).trim();
                        int fontSize = level == 1 ? 14 : (level == 2 ? 12 : 11);
                        Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize, SLATE_BLUE);
                        Paragraph hPara = new Paragraph(headingText, hFont);
                        hPara.setSpacingBefore(10);
                        hPara.setSpacingAfter(4);
                        document.add(hPara);
                        continue;
                    }

                    // Bullet / Numbered lists (- , * , 1. , 2. )
                    boolean isList = trimmed.matches("^([\\-\\*\u2022]|\\d+[\\.\\)])\\s+.*");
                    String cleanLine = trimmed;
                    String listPrefix = "";
                    if (isList) {
                        Pattern p = Pattern.compile("^([\\-\\*\u2022]|\\d+[\\.\\)])\\s+");
                        Matcher m = p.matcher(trimmed);
                        if (m.find()) {
                            listPrefix = m.group().trim() + " ";
                            cleanLine = trimmed.substring(m.end()).trim();
                        }
                    }

                    Paragraph p = new Paragraph();
                    p.setLeading(13);
                    p.setSpacingAfter(4);

                    if (isList) {
                        p.setIndentationLeft(14);
                        p.add(new Chunk(listPrefix.equals("- ") || listPrefix.equals("* ") ? "\u2022 " : listPrefix,
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SLATE_BLUE)));
                    }

                    // Parse inline bold/code/italic formatting
                    appendFormattedPdfChunks(p, cleanLine);
                    document.add(p);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF document: " + e.getMessage(), e);
        }
    }

    public byte[] generateDocx(String title, String content) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Title Header
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setSpacingAfter(120);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title != null && !title.isBlank() ? title : "MRPL Technical Deliverable");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setColor("1A365D");
            titleRun.setFontFamily("Calibri");

            // Subtitle
            XWPFParagraph subPara = doc.createParagraph();
            subPara.setSpacingAfter(240);
            XWPFRun subRun = subPara.createRun();
            subRun.setText("Sovereign AI Workbench \u2022 Mangalore Refinery & Petrochemicals Ltd \u2022 " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            subRun.setFontSize(9);
            subRun.setColor("718096");
            subRun.setFontFamily("Calibri");

            // Content Body
            if (content != null && !content.isBlank()) {
                String[] lines = content.split("\r?\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    // Ignore underline markers
                    if (trimmed.matches("^[=\\-]{3,}$")) {
                        continue;
                    }

                    XWPFParagraph p = doc.createParagraph();
                    p.setSpacingAfter(80);

                    // Headings
                    if (trimmed.startsWith("#")) {
                        int level = 0;
                        while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                            level++;
                        }
                        p.setSpacingBefore(180);
                        p.setSpacingAfter(80);
                        XWPFRun hRun = p.createRun();
                        hRun.setFontFamily("Calibri");
                        hRun.setText(trimmed.substring(level).trim());
                        hRun.setBold(true);
                        hRun.setFontSize(level == 1 ? 15 : (level == 2 ? 13 : 11));
                        hRun.setColor("2B6CB0");
                        continue;
                    }

                    // List items
                    boolean isList = trimmed.matches("^([\\-\\*\u2022]|\\d+[\\.\\)])\\s+.*");
                    String cleanLine = trimmed;
                    String listPrefix = "";
                    if (isList) {
                        Pattern pattern = Pattern.compile("^([\\-\\*\u2022]|\\d+[\\.\\)])\\s+");
                        Matcher m = pattern.matcher(trimmed);
                        if (m.find()) {
                            listPrefix = m.group().trim() + " ";
                            cleanLine = trimmed.substring(m.end()).trim();
                        }
                        p.setIndentationLeft(360);
                        XWPFRun bulletRun = p.createRun();
                        bulletRun.setFontFamily("Calibri");
                        bulletRun.setBold(true);
                        bulletRun.setColor("2B6CB0");
                        bulletRun.setText(listPrefix.equals("- ") || listPrefix.equals("* ") ? "\u2022 " : listPrefix);
                    }

                    // Parse inline bold/italic/code in Docx
                    appendFormattedDocxRuns(p, cleanLine);
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DOCX document: " + e.getMessage(), e);
        }
    }

    private void appendFormattedPdfChunks(Paragraph paragraph, String text) {
        Matcher matcher = INLINE_PATTERN.matcher(text);
        int lastIdx = 0;
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, DARK_GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_GRAY);
        Font codeFont = FontFactory.getFont(FontFactory.COURIER, 9, new Color(199, 37, 78));
        Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, DARK_GRAY);

        while (matcher.find()) {
            if (matcher.start() > lastIdx) {
                paragraph.add(new Chunk(text.substring(lastIdx, matcher.start()), regularFont));
            }
            if (matcher.group(2) != null) { // **bold**
                paragraph.add(new Chunk(matcher.group(2), boldFont));
            } else if (matcher.group(4) != null) { // `code`
                paragraph.add(new Chunk(matcher.group(4), codeFont));
            } else if (matcher.group(6) != null) { // *italic*
                paragraph.add(new Chunk(matcher.group(6), italicFont));
            }
            lastIdx = matcher.end();
        }

        if (lastIdx < text.length()) {
            paragraph.add(new Chunk(text.substring(lastIdx), regularFont));
        }
    }

    private void appendFormattedDocxRuns(XWPFParagraph paragraph, String text) {
        Matcher matcher = INLINE_PATTERN.matcher(text);
        int lastIdx = 0;

        while (matcher.find()) {
            if (matcher.start() > lastIdx) {
                XWPFRun r = paragraph.createRun();
                r.setFontFamily("Calibri");
                r.setFontSize(10.5);
                r.setColor("2D3748");
                r.setText(text.substring(lastIdx, matcher.start()));
            }
            if (matcher.group(2) != null) { // **bold**
                XWPFRun r = paragraph.createRun();
                r.setFontFamily("Calibri");
                r.setFontSize(10.5);
                r.setColor("1A202C");
                r.setBold(true);
                r.setText(matcher.group(2));
            } else if (matcher.group(4) != null) { // `code`
                XWPFRun r = paragraph.createRun();
                r.setFontFamily("Consolas");
                r.setFontSize(9.5);
                r.setColor("9B2C2C");
                r.setText(matcher.group(4));
            } else if (matcher.group(6) != null) { // *italic*
                XWPFRun r = paragraph.createRun();
                r.setFontFamily("Calibri");
                r.setFontSize(10.5);
                r.setColor("2D3748");
                r.setItalic(true);
                r.setText(matcher.group(6));
            }
            lastIdx = matcher.end();
        }

        if (lastIdx < text.length()) {
            XWPFRun r = paragraph.createRun();
            r.setFontFamily("Calibri");
            r.setFontSize(10.5);
            r.setColor("2D3748");
            r.setText(text.substring(lastIdx));
        }
    }
}
