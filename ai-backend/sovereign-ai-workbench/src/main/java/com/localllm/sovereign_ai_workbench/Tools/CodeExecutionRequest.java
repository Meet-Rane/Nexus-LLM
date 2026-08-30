package com.localllm.sovereign_ai_workbench.Tools;

import java.util.Map;

public class CodeExecutionRequest {

    private String language;
    private Map<String, String> files;
    private String entryFile;

    public CodeExecutionRequest() {
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