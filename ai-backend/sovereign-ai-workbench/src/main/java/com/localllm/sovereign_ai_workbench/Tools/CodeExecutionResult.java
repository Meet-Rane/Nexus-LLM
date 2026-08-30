package com.localllm.sovereign_ai_workbench.Tools;

import java.util.List;

public class CodeExecutionResult {

    private final int exitCode;
    private final String output;
    private final boolean timedOut;
    private final List<String> artifacts;

    public CodeExecutionResult(
            int exitCode,
            String output,
            boolean timedOut,
            List<String> artifacts) {

        this.exitCode = exitCode;
        this.output = output;
        this.timedOut = timedOut;
        this.artifacts = artifacts;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public List<String> getArtifacts() {
        return artifacts;
    }
}