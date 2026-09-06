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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    public byte[] generateXlsx(String title, String content) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");
            sheet.setDisplayGridlines(false);
            sheet.setAutobreaks(true);

            List<List<String>> table = parseSpreadsheetRows(content);
            int columnCount = Math.max(2, table.stream().mapToInt(List::size).max().orElse(2));

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(27);
            XSSFCell titleCell = (XSSFCell) titleRow.createCell(0);
            titleCell.setCellValue(title != null && !title.isBlank() ? title : "MRPL Technical Workbook");
            titleCell.setCellStyle(createTitleCellStyle(workbook));

            Row contextRow = sheet.createRow(1);
            XSSFCell contextCell = (XSSFCell) contextRow.createCell(0);
            contextCell.setCellValue("Generated locally by Nexus Sovereign AI · "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            contextCell.setCellStyle(createContextCellStyle(workbook));

            int tableStart = 3;
            CellStyle headerStyle = createHeaderCellStyle(workbook);
            CellStyle bodyStyle = createBodyCellStyle(workbook, false);
            CellStyle alternateStyle = createBodyCellStyle(workbook, true);
            CellStyle highRiskStyle = createRiskCellStyle(workbook, new Color(254, 226, 226), new Color(153, 27, 27));
            CellStyle mediumRiskStyle = createRiskCellStyle(workbook, new Color(254, 243, 199), new Color(146, 64, 14));

            for (int rowIndex = 0; rowIndex < table.size(); rowIndex++) {
                Row row = sheet.createRow(tableStart + rowIndex);
                row.setHeightInPoints(rowIndex == 0 ? 25 : 22);
                List<String> values = table.get(rowIndex);
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    XSSFCell cell = (XSSFCell) row.createCell(columnIndex);
                    String value = columnIndex < values.size() ? values.get(columnIndex).trim() : "";
                    setTypedCellValue(cell, value);
                    if (rowIndex == 0) {
                        cell.setCellStyle(headerStyle);
                    } else {
                        String lower = value.toLowerCase(Locale.ROOT);
                        if (lower.equals("high") || lower.equals("critical")) {
                            cell.setCellStyle(highRiskStyle);
                        } else if (lower.equals("medium")) {
                            cell.setCellStyle(mediumRiskStyle);
                        } else {
                            cell.setCellStyle(rowIndex % 2 == 0 ? alternateStyle : bodyStyle);
                        }
                    }
                }
            }

            if (!table.isEmpty()) {
                sheet.createFreezePane(0, tableStart + 1);
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                        tableStart, tableStart + table.size() - 1, 0, columnCount - 1));
            }
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
                int boundedWidth = Math.min(Math.max(sheet.getColumnWidth(columnIndex) + 768, 12 * 256), 45 * 256);
                sheet.setColumnWidth(columnIndex, boundedWidth);
            }
            sheet.getPrintSetup().setLandscape(columnCount > 5);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            workbook.getProperties().getCoreProperties().setTitle(title);
            workbook.getProperties().getCoreProperties().setCreator("Nexus Sovereign AI Workbench");
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel workbook: " + e.getMessage(), e);
        }
    }

    public byte[] generatePptx(String title, String content) {
        try (XMLSlideShow presentation = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            presentation.setPageSize(new Dimension(960, 540));
            String deckTitle = title != null && !title.isBlank() ? title : "MRPL Technical Presentation";

            XSLFSlide cover = presentation.createSlide();
            setSlideBackground(cover);
            addTextBox(cover, deckTitle, 70, 142, 820, 125, 42, true, new Color(26, 54, 93));
            addTextBox(cover, "Sovereign AI Workbench · On-premise generated deliverable", 72, 278, 815, 45, 16, false, new Color(90, 102, 116));
            addFooter(cover, 1);

            List<SlideSection> sections = parseSlideSections(deckTitle, content);
            int slideNumber = 2;
            for (SlideSection section : sections) {
                List<String> items = section.items().isEmpty() ? List.of("Content supplied in the request") : section.items();
                for (int start = 0; start < items.size(); start += 6) {
                    List<String> pageItems = items.subList(start, Math.min(start + 6, items.size()));
                    String slideTitle = start == 0 ? section.title() : section.title() + " (continued)";
                    XSLFSlide slide = presentation.createSlide();
                    setSlideBackground(slide);
                    addTextBox(slide, slideTitle, 64, 42, 832, 64, 32, true, new Color(26, 54, 93));
                    addBulletBox(slide, pageItems, 78, 125, 800, 335);
                    addFooter(slide, slideNumber++);
                }
            }

            presentation.getProperties().getCoreProperties().setTitle(deckTitle);
            presentation.getProperties().getCoreProperties().setCreator("Nexus Sovereign AI Workbench");
            presentation.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PowerPoint presentation: " + e.getMessage(), e);
        }
    }

    private static List<List<String>> parseSpreadsheetRows(String content) {
        List<String> lines = Arrays.stream((content == null ? "" : content).split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        List<List<String>> markdownRows = new ArrayList<>();
        for (String line : lines) {
            if (!line.contains("|") || line.matches("^\\|?[\\s:|\\-]+\\|?$")) continue;
            String normalized = line.replaceAll("^\\|", "").replaceAll("\\|$", "");
            markdownRows.add(Arrays.stream(normalized.split("\\|", -1)).map(String::trim).toList());
        }
        if (markdownRows.size() >= 2) return markdownRows;

        List<List<String>> csvRows = new ArrayList<>();
        for (String line : lines) {
            if (line.contains(",") && !line.startsWith("#")) csvRows.add(parseCsvLine(line));
        }
        if (csvRows.size() >= 2) return csvRows;

        List<List<String>> structured = new ArrayList<>();
        structured.add(List.of("Section", "Details"));
        String section = "Overview";
        for (String line : lines) {
            if (line.startsWith("#")) {
                section = cleanMarkup(line.replaceFirst("^#+\\s*", ""));
            } else if (!line.matches("^[=\\-]{3,}$")) {
                structured.add(List.of(section, cleanMarkup(line.replaceFirst("^([\\-*•]|\\d+[.)])\\s+", ""))));
            }
        }
        if (structured.size() == 1) structured.add(List.of("Overview", "No structured rows were supplied."));
        return structured;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (value == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private static void setTypedCellValue(XSSFCell cell, String value) {
        if (value.matches("[-+]?\\d+(\\.\\d+)?")) {
            try {
                cell.setCellValue(Double.parseDouble(value));
                return;
            } catch (NumberFormatException ignored) {
                // Keep unusually large numbers as text.
            }
        }
        cell.setCellValue(cleanMarkup(value));
    }

    private static XSSFCellStyle createTitleCellStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 18);
        font.setBold(true);
        font.setColor(new XSSFColor(new Color(26, 54, 93), null));
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static XSSFCellStyle createContextCellStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 9);
        font.setItalic(true);
        font.setColor(new XSSFColor(new Color(113, 128, 150), null));
        style.setFont(font);
        return style;
    }

    private static XSSFCellStyle createHeaderCellStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new Color(26, 54, 93), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.WHITE.getIndex());
        return style;
    }

    private static XSSFCellStyle createBodyCellStyle(XSSFWorkbook workbook, boolean alternate) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setColor(new XSSFColor(new Color(45, 55, 72), null));
        style.setFont(font);
        if (alternate) {
            style.setFillForegroundColor(new XSSFColor(new Color(247, 249, 252), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.HAIR);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private static XSSFCellStyle createRiskCellStyle(XSSFWorkbook workbook, Color fill, Color text) {
        XSSFCellStyle style = createBodyCellStyle(workbook, false);
        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(new XSSFColor(text, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(fill, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static List<SlideSection> parseSlideSections(String deckTitle, String content) {
        List<SlideSection> sections = new ArrayList<>();
        String currentTitle = "Overview";
        List<String> currentItems = new ArrayList<>();
        for (String rawLine : (content == null ? "" : content).split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank() || line.matches("^[=\\-]{3,}$")) continue;
            if (line.startsWith("#")) {
                String heading = cleanMarkup(line.replaceFirst("^#+\\s*", ""));
                if (heading.equalsIgnoreCase(deckTitle) && sections.isEmpty() && currentItems.isEmpty()) continue;
                if (!currentItems.isEmpty()) {
                    sections.add(new SlideSection(currentTitle, List.copyOf(currentItems)));
                    currentItems.clear();
                }
                currentTitle = heading.isBlank() ? "Overview" : heading;
            } else {
                String item = cleanMarkup(line.replaceFirst("^([\\-*•]|\\d+[.)])\\s+", ""));
                if (!item.isBlank()) currentItems.add(item);
            }
        }
        if (!currentItems.isEmpty()) sections.add(new SlideSection(currentTitle, List.copyOf(currentItems)));
        if (sections.isEmpty()) sections.add(new SlideSection("Overview", List.of("No detailed slide content was supplied.")));
        return sections;
    }

    private static void setSlideBackground(XSLFSlide slide) {
        slide.getBackground().setFillColor(new Color(248, 250, 252));
    }

    private static void addTextBox(XSLFSlide slide, String text, double x, double y, double width, double height,
                                   double fontSize, boolean bold, Color color) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        box.setText(cleanMarkup(text));
        box.setWordWrap(true);
        for (XSLFTextParagraph paragraph : box.getTextParagraphs()) {
            paragraph.setSpaceAfter(0d);
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                run.setFontFamily("Arial");
                run.setFontSize(fontSize);
                run.setBold(bold);
                run.setFontColor(color);
            }
        }
    }

    private static void addBulletBox(XSLFSlide slide, List<String> items, double x, double y, double width, double height) {
        XSLFTextBox body = slide.createTextBox();
        body.setAnchor(new Rectangle2D.Double(x, y, width, height));
        body.setWordWrap(true);
        body.clearText();
        for (String item : items) {
            XSLFTextParagraph paragraph = body.addNewTextParagraph();
            paragraph.setBullet(true);
            paragraph.setLeftMargin(22d);
            paragraph.setIndent(-10d);
            paragraph.setSpaceAfter(12d);
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(cleanMarkup(item));
            run.setFontFamily("Arial");
            run.setFontSize(18d);
            run.setFontColor(new Color(45, 55, 72));
        }
    }

    private static void addFooter(XSLFSlide slide, int slideNumber) {
        addTextBox(slide, "Nexus Sovereign AI · Local artifact", 64, 500, 500, 20, 9, false, new Color(113, 128, 150));
        addTextBox(slide, String.valueOf(slideNumber), 855, 500, 40, 20, 9, false, new Color(113, 128, 150));
    }

    private static String cleanMarkup(String text) {
        return text == null ? "" : text.replace("**", "").replace("`", "").replace("__", "").trim();
    }

    private record SlideSection(String title, List<String> items) {
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
