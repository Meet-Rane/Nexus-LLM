package com.localllm.sovereign_ai_workbench.Service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class NetworkAuditService {

    private static final int MAX_EVENTS = 100;
    private final ConcurrentLinkedDeque<NetworkAuditEvent> events = new ConcurrentLinkedDeque<>();

    public void record(String target, String endpoint, String operation) {
        events.addFirst(new NetworkAuditEvent(
                Instant.now().toString(),
                target,
                endpoint,
                operation,
                isLoopbackUrl(endpoint)
        ));

        while (events.size() > MAX_EVENTS) {
            events.pollLast();
        }
    }

    public List<NetworkAuditEvent> recentEvents() {
        return new ArrayList<>(events);
    }

    private boolean isLoopbackUrl(String value) {
        try {
            String host = URI.create(value).getHost();
            return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        } catch (Exception ignored) {
            return false;
        }
    }

    public record NetworkAuditEvent(
            String timestamp,
            String target,
            String endpoint,
            String operation,
            boolean loopback
    ) {}
}
