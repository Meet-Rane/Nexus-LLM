package com.localllm.sovereign_ai_workbench.Router;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelRouterTests {

    private final ModelRouter router = new ModelRouter(
            null,
            null,
            null,
            "qwen2.5-coder:7b",
            "llama3.1:8b",
            "deepseek-r1:8b",
            "ollama",
            "http://localhost:11434",
            "http://localhost:1234"
    );

    @Test
    void routesProgrammingTasksToCodingModel() {
        RouteDecision decision = router.selectModel("test", "Write a Python script for this CSV dataset");

        assertEquals("CODING", decision.category());
        assertEquals("qwen2.5-coder:7b", decision.model());
    }

    @Test
    void routesEngineeringCalculationsToReasoningModel() {
        RouteDecision decision = router.selectModel("test", "Calculate pressure drop through this pipe");

        assertEquals("CALCULATION_REASONING", decision.category());
        assertEquals("deepseek-r1:8b", decision.model());
    }

    @Test
    void routesFormalDeliverablesToGeneralModel() {
        RouteDecision decision = router.selectModel("test", "Draft an approval note for the inspection finding");

        assertEquals("DOCUMENT_APPROVAL", decision.category());
        assertEquals("llama3.1:8b", decision.model());
    }

    @Test
    void routesUploadedManualQuestionsToGeneralModelWithoutClassifierCall() {
        RouteDecision decision = router.selectModel("test", "Search the uploaded internal manual for lockout guidance");

        assertEquals("DOCUMENT_APPROVAL", decision.category());
        assertEquals("llama3.1:8b", decision.model());
    }
}
