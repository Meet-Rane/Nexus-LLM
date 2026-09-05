package com.localllm.sovereign_ai_workbench.Tools;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteFileRequest {

    @JsonProperty("path")
    @JsonAlias({"filePath", "filename", "fileName", "file", "name"})
    private String path;

    @JsonProperty("content")
    @JsonAlias({"body", "text", "markdown", "data", "code"})
    private String content;

    public WriteFileRequest() {
    }

    @JsonCreator
    public WriteFileRequest(
            @JsonProperty("path") @JsonAlias({"filePath", "filename", "fileName", "file", "name"}) String path,
            @JsonProperty("content") @JsonAlias({"body", "text", "markdown", "data", "code"}) String content) {
        this.path = path;
        this.content = content;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WriteFileRequest fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return new WriteFileRequest("file.txt", "");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
                mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
                mapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);

                JsonNode node = mapper.readTree(trimmed);
                String p = extractField(node, "path", "filePath", "filename", "fileName", "file", "name");
                String c = extractField(node, "content", "body", "text", "markdown", "data", "code");
                if (c != null && !c.isBlank()) {
                    return new WriteFileRequest(p != null ? p : "file.txt", c);
                }
            } catch (Exception ignored) {
            }

            String path = extractRegex(trimmed, "\"(?:path|filePath|filename|fileName|file|name)\"\\s*:\\s*\"([^\"]+)\"");
            Pattern contentPattern = Pattern.compile("\"(?:content|body|text|markdown|data|code)\"\\s*:\\s*\"(.*)\"", Pattern.DOTALL);
            Matcher m = contentPattern.matcher(trimmed);
            if (m.find()) {
                String extractedContent = m.group(1).trim();
                if (extractedContent.endsWith("\"}")) {
                    extractedContent = extractedContent.substring(0, extractedContent.length() - 2);
                } else if (extractedContent.endsWith("\"")) {
                    extractedContent = extractedContent.substring(0, extractedContent.length() - 1);
                }
                extractedContent = extractedContent.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
                return new WriteFileRequest(path != null ? path : "file.txt", extractedContent);
            }
        }
        return new WriteFileRequest("file.txt", raw);
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
