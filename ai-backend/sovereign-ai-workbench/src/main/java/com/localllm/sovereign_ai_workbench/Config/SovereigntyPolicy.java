package com.localllm.sovereign_ai_workbench.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class SovereigntyPolicy {

    private final boolean enforceLocalOnly;
    private final String provider;
    private final String ollamaBaseUrl;
    private final String ragBaseUrl;

    public SovereigntyPolicy(
            @Value("${sovereign.enforce-local-only:true}") boolean enforceLocalOnly,
            @Value("${ai.provider}") String provider,
            @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl,
            @Value("${rag.service.base-url}") String ragBaseUrl
    ) {
        this.enforceLocalOnly = enforceLocalOnly;
        this.provider = provider;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ragBaseUrl = ragBaseUrl;
    }

    @PostConstruct
    public void validate() {
        if (!enforceLocalOnly) {
            return;
        }

        if (!"ollama".equalsIgnoreCase(provider)) {
            throw new IllegalStateException(
                    "Sovereign local-only mode requires ai.provider=ollama. "
                            + "Set SOVEREIGN_ENFORCE_LOCAL_ONLY=false only for an explicitly approved non-sovereign deployment."
            );
        }

        requireLoopback("Ollama", ollamaBaseUrl);
        requireLoopback("RAG", ragBaseUrl);
    }

    private void requireLoopback(String service, String value) {
        try {
            String host = URI.create(value).getHost();
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
                return;
            }
        } catch (Exception ignored) {
            // Report the same clear configuration error below.
        }

        throw new IllegalStateException(service + " must use a loopback URL in sovereign local-only mode: " + value);
    }
}
