package com.localllm.sovereign_ai_workbench.Tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadFileRequest {

    @JsonProperty("path")
    private String path;

    public ReadFileRequest() {
    }

    @JsonCreator
    public ReadFileRequest(@JsonProperty("path") String path) {
        this.path = path;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ReadFileRequest fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ReadFileRequest("file.txt");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(trimmed);
                String p = node.has("path") ? node.get("path").asText() : "file.txt";
                return new ReadFileRequest(p);
            } catch (Exception ignored) {
            }
        }
        return new ReadFileRequest(raw);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
