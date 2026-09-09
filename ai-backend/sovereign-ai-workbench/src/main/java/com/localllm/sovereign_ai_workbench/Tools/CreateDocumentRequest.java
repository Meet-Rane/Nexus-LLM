package com.localllm.sovereign_ai_workbench.Tools;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateDocumentRequest {

    @JsonProperty("path")
    @JsonAlias({"filePath", "filename", "fileName", "file", "name"})
    private String path;

    @JsonProperty("title")
    @JsonAlias({"docTitle", "documentTitle", "header", "heading"})
    private String title;

    @JsonProperty("content")
    @JsonAlias({"body", "text", "markdown", "data", "code"})
    private String content;

    public CreateDocumentRequest() {
    }

    @JsonCreator
    public CreateDocumentRequest(
            @JsonProperty("path") @JsonAlias({"filePath", "filename", "fileName", "file", "name"}) String path,
            @JsonProperty("title") @JsonAlias({"docTitle", "documentTitle", "header", "heading"}) String title,
            @JsonProperty("content") @JsonAlias({"body", "text", "markdown", "data", "code"}) String content) {
        this.path = path;
        this.title = title;
        this.content = content;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CreateDocumentRequest fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return new CreateDocumentRequest("document.pdf", "Document", "");
        }
        String trimmed = raw.trim();

        // 1. Try standard Jackson ObjectMapper with permissive features
        if (trimmed.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
                mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
                mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);

                JsonNode node = mapper.readTree(trimmed);
                String p = extractField(node, "path", "filePath", "filename", "fileName", "file", "name");
                String t = extractField(node, "title", "docTitle", "documentTitle", "header", "heading");
                String c = extractField(node, "content", "body", "text", "markdown", "data", "code");
                if (c != null && !c.isBlank()) {
                    return new CreateDocumentRequest(p != null ? p : "document.pdf", t != null ? t : "Document", c);
                }
            } catch (Exception ignored) {
            }

            // 2. Regex fallback for malformed JSON containing unescaped newlines/quotes
            String path = extractRegex(trimmed, "\"(?:path|filePath|filename|fileName|file|name)\"\\s*:\\s*\"([^\"]+)\"");
            String title = extractRegex(trimmed, "\"(?:title|docTitle|documentTitle|header|heading)\"\\s*:\\s*\"([^\"]+)\"");

            Pattern contentPattern = Pattern.compile("\"(?:content|body|text|markdown)\"\\s*:\\s*\"(.*)\"", Pattern.DOTALL);
            Matcher m = contentPattern.matcher(trimmed);
            if (m.find()) {
                String extractedContent = m.group(1).trim();
                if (extractedContent.endsWith("\"}")) {
                    extractedContent = extractedContent.substring(0, extractedContent.length() - 2);
                } else if (extractedContent.endsWith("\"")) {
                    extractedContent = extractedContent.substring(0, extractedContent.length() - 1);
                }
                extractedContent = extractedContent.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
                return new CreateDocumentRequest(path != null ? path : "document.pdf", title != null ? title : "Document", extractedContent);
            }
        }

        // If raw plain text
        return new CreateDocumentRequest("document.pdf", "Document", raw);
    }

    private static String extractRegex(String text, String regex) {
        try {
            Matcher m = Pattern.compile(regex).matcher(text);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText();
            }
        }
        return null;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
