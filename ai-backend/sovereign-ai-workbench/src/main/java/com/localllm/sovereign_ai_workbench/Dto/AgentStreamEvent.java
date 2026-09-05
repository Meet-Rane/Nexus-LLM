package com.localllm.sovereign_ai_workbench.Dto;

public record AgentStreamEvent(
        String type,           // ROUTER, TOOL_START, TOOL_COMPLETE, ARTIFACT_CREATED, TEXT, DONE, ERROR
        String model,          // selected model name (if type == ROUTER)
        String toolName,       // name of tool (if type == TOOL_START or TOOL_COMPLETE)
        String detail,         // details, input args summary, or completion info
        String content,        // text token (if type == TEXT)
        ArtifactDto artifact   // created/updated artifact metadata (if type == ARTIFACT_CREATED or TOOL_COMPLETE)
) {
    public static AgentStreamEvent router(String model, String detail) {
        return new AgentStreamEvent("ROUTER", model, null, detail, null, null);
    }

    public static AgentStreamEvent toolStart(String toolName, String detail) {
        return new AgentStreamEvent("TOOL_START", null, toolName, detail, null, null);
    }

    public static AgentStreamEvent toolComplete(String toolName, String detail) {
        return new AgentStreamEvent("TOOL_COMPLETE", null, toolName, detail, null, null);
    }

    public static AgentStreamEvent toolCompleteWithArtifact(String toolName, String detail, ArtifactDto artifact) {
        return new AgentStreamEvent("TOOL_COMPLETE", null, toolName, detail, null, artifact);
    }

    public static AgentStreamEvent artifactCreated(ArtifactDto artifact) {
        return new AgentStreamEvent("ARTIFACT_CREATED", null, null, "Artifact generated: " + artifact.path(), null, artifact);
    }

    public static AgentStreamEvent text(String content) {
        return new AgentStreamEvent("TEXT", null, null, null, content, null);
    }

    public static AgentStreamEvent done() {
        return new AgentStreamEvent("DONE", null, null, "Completed", null, null);
    }

    public static AgentStreamEvent error(String detail) {
        return new AgentStreamEvent("ERROR", null, null, detail, null, null);
    }
}
