package com.localllm.sovereign_ai_workbench.Config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SovereigntyPolicyTests {

    @Test
    void acceptsLoopbackOllamaAndRagEndpoints() {
        SovereigntyPolicy policy = new SovereigntyPolicy(
                true,
                "ollama",
                "http://127.0.0.1:11434",
                "http://localhost:8001"
        );

        assertDoesNotThrow(policy::validate);
    }

    @Test
    void rejectsExternalEndpointsWhenLocalOnlyModeIsEnabled() {
        SovereigntyPolicy policy = new SovereigntyPolicy(
                true,
                "ollama",
                "https://example.com",
                "http://localhost:8001"
        );

        assertThrows(IllegalStateException.class, policy::validate);
    }
}
