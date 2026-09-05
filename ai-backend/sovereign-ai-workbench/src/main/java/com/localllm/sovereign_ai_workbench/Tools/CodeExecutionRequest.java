package com.localllm.sovereign_ai_workbench.Tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class CodeExecutionRequest {

    @JsonProperty("language")
    private String language;

    @JsonProperty("files")
    private Map<String, String> files;

    @JsonProperty("entryFile")
    private String entryFile;

    public CodeExecutionRequest() {
    }

    @JsonCreator
    public CodeExecutionRequest(
            @JsonProperty("language") String language,
            @JsonProperty("files") Map<String, String> files,
            @JsonProperty("entryFile") String entryFile) {
        this.language = language;
        this.files = files;
        this.entryFile = entryFile;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CodeExecutionRequest fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return new CodeExecutionRequest("python", new HashMap<>(), "main.py");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(trimmed);
                String lang = node.has("language") ? node.get("language").asText() : "python";
                String entry = node.has("entryFile") ? node.get("entryFile").asText() : "main.py";
                Map<String, String> f = new HashMap<>();
                if (node.has("files")) {
                    f = mapper.convertValue(node.get("files"), new TypeReference<Map<String, String>>() {});
                }
                return new CodeExecutionRequest(lang, f, entry);
            } catch (Exception ignored) {
            }
        }
        Map<String, String> f = new HashMap<>();
        f.put("main.py", raw);
        return new CodeExecutionRequest("python", f, "main.py");
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }

    public String getEntryFile() {
        return entryFile;
    }

    public void setEntryFile(String entryFile) {
        this.entryFile = entryFile;
    }
}