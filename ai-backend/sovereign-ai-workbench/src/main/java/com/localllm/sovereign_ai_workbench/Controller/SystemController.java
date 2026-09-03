package com.localllm.sovereign_ai_workbench.Controller;

import com.localllm.sovereign_ai_workbench.Service.NetworkAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final String provider;
    private final String codingModel;
    private final String generalModel;
    private final String ollamaBaseUrl;
    private final String ragBaseUrl;
    private final String artifactStorage;
    private final String dockerCommand;
    private final boolean localOnlyPolicyEnforced;
    private final boolean sandboxNetworkDisabled;
    private final NetworkAuditService networkAuditService;

    public SystemController(
            @Value("${ai.provider}") String provider,
            @Value("${ai.coding.model}") String codingModel,
            @Value("${ai.general.model}") String generalModel,
            @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl,
            @Value("${rag.service.base-url}") String ragBaseUrl,
            @Value("${sandbox.artifact-storage}") String artifactStorage,
            @Value("${sandbox.docker.command}") String dockerCommand,
            @Value("${sovereign.enforce-local-only:true}") boolean localOnlyPolicyEnforced,
            @Value("${sandbox.network-disabled:true}") boolean sandboxNetworkDisabled,
            NetworkAuditService networkAuditService
    ) {
        this.provider = provider;
        this.codingModel = codingModel;
        this.generalModel = generalModel;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ragBaseUrl = ragBaseUrl;
        this.artifactStorage = artifactStorage;
        this.dockerCommand = dockerCommand;
        this.localOnlyPolicyEnforced = localOnlyPolicyEnforced;
        this.sandboxNetworkDisabled = sandboxNetworkDisabled;
        this.networkAuditService = networkAuditService;
    }

    @GetMapping("/status")
    public SystemStatus status() {
        return new SystemStatus(
                provider,
                List.of(
                        new ModelRoute("Coding", codingModel),
                        new ModelRoute("General and documents", generalModel)
                ),
                List.of(
                        "search_knowledge_base",
                        "execute_python_code",
                        "create_file",
                        "read_file",
                        "write_file",
                        "list_files"
                ),
                ollamaBaseUrl,
                ragBaseUrl,
                artifactStorage,
                commandAvailable(dockerCommand, "version"),
                commandAvailable("tesseract", "--version"),
                isLocalRuntime(),
                localOnlyPolicyEnforced,
                sandboxNetworkDisabled,
                true,
                false
        );
    }

    @GetMapping("/network-audit")
    public List<NetworkAuditService.NetworkAuditEvent> networkAudit() {
        return networkAuditService.recentEvents();
    }

    private boolean isLocalRuntime() {
        return "ollama".equalsIgnoreCase(provider)
                && isLoopbackUrl(ollamaBaseUrl)
                && isLoopbackUrl(ragBaseUrl);
    }

    private boolean isLoopbackUrl(String value) {
        try {
            String host = URI.create(value).getHost();
            return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean commandAvailable(String command, String argument) {
        Process process = null;
        try {
            process = new ProcessBuilder(command, argument)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public record ModelRoute(String taskType, String model) {}

    public record SystemStatus(
            String provider,
            List<ModelRoute> modelRoutes,
            List<String> tools,
            String ollamaBaseUrl,
            String ragBaseUrl,
            String artifactStorage,
            boolean dockerAvailable,
            boolean tesseractAvailable,
            boolean localRuntimeConfigured,
            boolean localOnlyPolicyEnforced,
            boolean sandboxNetworkDisabled,
            boolean applicationEgressAuditEnabled,
            boolean egressMonitorEnabled
    ) {}
}
