package com.localllm.sovereign_ai_workbench.Router;

public record RouteDecision(
        String model,
        String category,
        String reason
) {}
