package com.localllm.sovereign_ai_workbench.Tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class CodeExecutionRequest {

    private String language;
    private Map<String, String> files;
    private String entryFile;

    public CodeExecutionRequest() {
    }

    // String constructor to handle LLM models that pass tool call arguments as a JSON string
    public CodeExecutionRequest(String jsonString) {
        if (jsonString != null && jsonString.trim().startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                CodeExecutionRequest parsed = mapper.readValue(jsonString, CodeExecutionRequest.class);
                this.language = parsed.language;
                this.files = parsed.files;
                this.entryFile = parsed.entryFile;
            } catch (Exception ignored) {
            }
        }
    }

    public CodeExecutionRequest(
            String language,
            Map<String, String> files,
            String entryFile) {

        this.language = language;
        this.files = files;
        this.entryFile = entryFile;
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