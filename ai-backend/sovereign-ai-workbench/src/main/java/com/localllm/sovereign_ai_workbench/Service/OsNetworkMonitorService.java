package com.localllm.sovereign_ai_workbench.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Samples Windows' system-wide TCP table and retains connection evidence in memory.
 *
 * This deliberately uses the operating system's own netstat view instead of only
 * instrumenting Nexus HTTP clients, so connections created by any process are visible.
 * It is a connection-state sampler rather than packet capture: it does not inspect
 * payloads and cannot prove the absence of very short-lived connections between samples.
 */
@Service
public class OsNetworkMonitorService {

    private static final int MAX_HOST_OBSERVATIONS = 2_000;
    private static final int MAX_NEXUS_OBSERVATIONS = 500;
    private static final int MAX_RETURNED_CONNECTIONS = 80;
    private static final Pattern PROCESS_ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)");

    private final boolean enabled;
    private final String command;
    private final boolean supported;
    private final String platform;
    private final Path nexusStatePath;
    private final Map<String, MutableObservation> observations = new LinkedHashMap<>();
    private final Map<String, MutableObservation> nexusObservations = new LinkedHashMap<>();

    private Instant startedAt = Instant.now();
    private Instant lastSampleAt;
    private long sampleCount;
    private int trackedNexusProcesses;
    private boolean running;
    private String lastError;
    private List<ConnectionSample> currentConnections = List.of();

    public OsNetworkMonitorService(
            @Value("${os.network.monitor.enabled:true}") boolean enabled,
            @Value("${os.network.monitor.command:netstat}") String command,
            @Value("${os.network.monitor.nexus-state-path:storage/runtime/nexus-processes.json}") String nexusStatePath) {
        this.enabled = enabled;
        this.command = command;
        this.nexusStatePath = Path.of(nexusStatePath);
        this.platform = System.getProperty("os.name", "unknown");
        this.supported = platform.toLowerCase(Locale.ROOT).contains("windows");
        if (!enabled) {
            this.lastError = "OS network monitoring is disabled by configuration.";
        } else if (!supported) {
            this.lastError = "The bundled OS monitor currently supports Windows netstat output.";
        }
    }

    @Scheduled(
            initialDelayString = "${os.network.monitor.initial-delay-ms:750}",
            fixedDelayString = "${os.network.monitor.poll-ms:2000}")
    public synchronized void sample() {
        if (!enabled || !supported) {
            running = false;
            return;
        }

        Process process = null;
        try {
            process = new ProcessBuilder(command, "-ano", "-p", "tcp")
                    .redirectErrorStream(true)
                    .start();

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("netstat sampling timed out after 3 seconds");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("netstat exited with code " + process.exitValue());
            }

            Instant observedAt = Instant.now();
            Set<Long> nexusProcessIds = loadNexusProcessIds();
            Map<Long, String> processNames = new HashMap<>();
            List<ConnectionSample> parsed = lines.stream()
                    .map(OsNetworkMonitorService::parseNetstatLine)
                    .flatMap(Optional::stream)
                    .map(connection -> connection
                            .withProcessName(processNames.computeIfAbsent(
                                    connection.pid(), OsNetworkMonitorService::resolveProcessName))
                            .withNexusProcess(nexusProcessIds.contains(connection.pid())))
                    .toList();

            // TIME_WAIT entries are valuable evidence, but they are already closed and
            // should not inflate the "current connection" counters.
            currentConnections = parsed.stream()
                    .filter(connection -> !"TIME_WAIT".equals(connection.state()))
                    .toList();
            for (ConnectionSample connection : parsed) {
                String key = connection.protocol() + "|" + connection.pid() + "|"
                        + connection.localAddress() + "|" + connection.remoteAddress();
                updateObservation(observations, key, connection, observedAt);
                if (connection.nexusProcess()) {
                    updateObservation(nexusObservations, key, connection, observedAt);
                }
            }
            pruneOldestObservations(observations, MAX_HOST_OBSERVATIONS);
            pruneOldestObservations(nexusObservations, MAX_NEXUS_OBSERVATIONS);

            trackedNexusProcesses = nexusProcessIds.size();
            sampleCount++;
            lastSampleAt = observedAt;
            running = true;
            lastError = null;
        } catch (Exception error) {
            running = false;
            lastError = error.getMessage() == null ? "OS network sampling failed." : error.getMessage();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public synchronized MonitorSnapshot snapshot() {
        Map<String, MutableObservation> combinedHistory = new LinkedHashMap<>();
        nexusObservations.forEach(combinedHistory::put);
        observations.forEach(combinedHistory::putIfAbsent);
        List<ConnectionObservation> connectionHistory = combinedHistory.values().stream()
                .map(MutableObservation::toRecord)
                .sorted(Comparator
                        .comparing(ConnectionObservation::nexusProcess).reversed()
                        .thenComparing(ConnectionObservation::loopback)
                        .thenComparing(ConnectionObservation::lastSeen, Comparator.reverseOrder()))
                .limit(MAX_RETURNED_CONNECTIONS)
                .toList();

        int observedExternal = (int) observations.values().stream()
                .filter(observation -> !observation.loopback)
                .count();
        int currentExternal = (int) currentConnections.stream()
                .filter(connection -> !connection.loopback())
                .count();
        int currentNexus = (int) currentConnections.stream()
                .filter(ConnectionSample::nexusProcess)
                .count();
        int currentNexusExternal = (int) currentConnections.stream()
                .filter(ConnectionSample::nexusProcess)
                .filter(connection -> !connection.loopback())
                .count();
        int observedNexusExternal = (int) nexusObservations.values().stream()
                .filter(observation -> !observation.loopback)
                .count();

        return new MonitorSnapshot(
                enabled,
                supported,
                running,
                platform,
                "Windows netstat TCP table",
                startedAt.toString(),
                lastSampleAt == null ? null : lastSampleAt.toString(),
                sampleCount,
                trackedNexusProcesses,
                currentConnections.size(),
                currentConnections.size() - currentExternal,
                currentExternal,
                currentNexus,
                currentNexus - currentNexusExternal,
                currentNexusExternal,
                observations.size(),
                observations.size() - observedExternal,
                observedExternal,
                nexusObservations.size(),
                nexusObservations.size() - observedNexusExternal,
                observedNexusExternal,
                connectionHistory,
                lastError
        );
    }

    public synchronized MonitorSnapshot reset() {
        observations.clear();
        nexusObservations.clear();
        currentConnections = List.of();
        sampleCount = 0;
        trackedNexusProcesses = 0;
        startedAt = Instant.now();
        lastSampleAt = null;
        lastError = null;
        sample();
        return snapshot();
    }

    public synchronized boolean isOperational() {
        return enabled && supported && running;
    }

    private static void updateObservation(
            Map<String, MutableObservation> target,
            String key,
            ConnectionSample connection,
            Instant observedAt) {
        MutableObservation existing = target.get(key);
        if (existing == null) {
            target.put(key, new MutableObservation(connection, observedAt));
        } else {
            existing.update(connection, observedAt);
        }
    }

    private static void pruneOldestObservations(
            Map<String, MutableObservation> target,
            int maximum) {
        while (target.size() > maximum) {
            String oldestKey = target.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().firstSeen))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestKey == null) {
                return;
            }
            target.remove(oldestKey);
        }
    }

    private Set<Long> loadNexusProcessIds() {
        Set<Long> processIds = new HashSet<>();
        processIds.add(ProcessHandle.current().pid());
        if (!Files.isRegularFile(nexusStatePath)) {
            return processIds;
        }

        try {
            Matcher processIdMatcher = PROCESS_ID_PATTERN.matcher(Files.readString(nexusStatePath));
            while (processIdMatcher.find()) {
                long processId = Long.parseLong(processIdMatcher.group(1));
                if (processId > 0 && ProcessHandle.of(processId).map(ProcessHandle::isAlive).orElse(false)) {
                    processIds.add(processId);
                }
            }
        } catch (Exception ignored) {
            // The state file is operational metadata and can be rewritten while a
            // sample is running. The backend PID still remains tracked in that case.
        }
        return processIds;
    }

    static Optional<ConnectionSample> parseNetstatLine(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 5 || !"TCP".equalsIgnoreCase(parts[0])) {
            return Optional.empty();
        }

        String state = parts[3].toUpperCase(Locale.ROOT);
        if ("LISTENING".equals(state)) {
            return Optional.empty();
        }

        long pid;
        try {
            pid = Long.parseLong(parts[4]);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }

        String remoteHost = endpointHost(parts[2]);
        if (remoteHost.isBlank() || "*".equals(remoteHost)
                || "0.0.0.0".equals(remoteHost) || "::".equals(remoteHost)) {
            return Optional.empty();
        }

        return Optional.of(new ConnectionSample(
                "TCP",
                parts[1],
                parts[2],
                state,
                pid,
                "PID " + pid,
                isLoopbackHost(remoteHost),
                false
        ));
    }

    private static String endpointHost(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "";
        }
        if (endpoint.startsWith("[")) {
            int closingBracket = endpoint.indexOf(']');
            if (closingBracket > 1) {
                return endpoint.substring(1, closingBracket);
            }
        }
        int lastColon = endpoint.lastIndexOf(':');
        return lastColon > 0 ? endpoint.substring(0, lastColon) : endpoint;
    }

    private static boolean isLoopbackHost(String host) {
        try {
            String normalized = host;
            int zoneIndex = normalized.indexOf('%');
            if (zoneIndex > 0) {
                normalized = normalized.substring(0, zoneIndex);
            }
            return InetAddress.getByName(normalized).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String resolveProcessName(long pid) {
        if (pid <= 0) {
            return "System";
        }
        return ProcessHandle.of(pid)
                .flatMap(handle -> handle.info().command())
                .map(commandPath -> {
                    try {
                        return Path.of(commandPath).getFileName().toString();
                    } catch (Exception ignored) {
                        return commandPath;
                    }
                })
                .orElse("PID " + pid);
    }

    record ConnectionSample(
            String protocol,
            String localAddress,
            String remoteAddress,
            String state,
            long pid,
            String processName,
            boolean loopback,
            boolean nexusProcess) {

        ConnectionSample withProcessName(String value) {
            return new ConnectionSample(protocol, localAddress, remoteAddress, state, pid, value, loopback, nexusProcess);
        }

        ConnectionSample withNexusProcess(boolean value) {
            return new ConnectionSample(protocol, localAddress, remoteAddress, state, pid, processName, loopback, value);
        }
    }

    private static final class MutableObservation {
        private final String protocol;
        private final String localAddress;
        private final String remoteAddress;
        private final long pid;
        private final boolean loopback;
        private boolean nexusProcess;
        private final Instant firstSeen;
        private Instant lastSeen;
        private String state;
        private String processName;
        private long samples;

        private MutableObservation(ConnectionSample sample, Instant observedAt) {
            this.protocol = sample.protocol();
            this.localAddress = sample.localAddress();
            this.remoteAddress = sample.remoteAddress();
            this.state = sample.state();
            this.pid = sample.pid();
            this.processName = sample.processName();
            this.loopback = sample.loopback();
            this.nexusProcess = sample.nexusProcess();
            this.firstSeen = observedAt;
            this.lastSeen = observedAt;
            this.samples = 1;
        }

        private void update(ConnectionSample sample, Instant observedAt) {
            this.state = sample.state();
            this.processName = sample.processName();
            this.nexusProcess = this.nexusProcess || sample.nexusProcess();
            this.lastSeen = observedAt;
            this.samples++;
        }

        private ConnectionObservation toRecord() {
            return new ConnectionObservation(
                    protocol,
                    localAddress,
                    remoteAddress,
                    state,
                    pid,
                    processName,
                    loopback,
                    nexusProcess,
                    firstSeen.toString(),
                    lastSeen.toString(),
                    samples
            );
        }
    }

    public record ConnectionObservation(
            String protocol,
            String localAddress,
            String remoteAddress,
            String state,
            long pid,
            String processName,
            boolean loopback,
            boolean nexusProcess,
            String firstSeen,
            String lastSeen,
            long samples) {
    }

    public record MonitorSnapshot(
            boolean enabled,
            boolean supported,
            boolean running,
            String platform,
            String captureMethod,
            String startedAt,
            String lastSampleAt,
            long sampleCount,
            int trackedNexusProcesses,
            int currentConnections,
            int currentLoopbackConnections,
            int currentExternalConnections,
            int currentNexusConnections,
            int currentNexusLoopbackConnections,
            int currentNexusExternalConnections,
            int observedConnections,
            int observedLoopbackConnections,
            int observedExternalConnections,
            int observedNexusConnections,
            int observedNexusLoopbackConnections,
            int observedNexusExternalConnections,
            List<ConnectionObservation> connections,
            String error) {
    }
}
