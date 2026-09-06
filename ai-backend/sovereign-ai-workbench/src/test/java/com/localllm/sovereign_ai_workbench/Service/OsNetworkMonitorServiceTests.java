package com.localllm.sovereign_ai_workbench.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsNetworkMonitorServiceTests {

    @Test
    void parsesWindowsLoopbackConnection() {
        var connection = OsNetworkMonitorService.parseNetstatLine(
                "  TCP    127.0.0.1:8090    127.0.0.1:55122    ESTABLISHED    1240")
                .orElseThrow();

        assertEquals(1240, connection.pid());
        assertEquals("127.0.0.1:55122", connection.remoteAddress());
        assertTrue(connection.loopback());
    }

    @Test
    void detectsExternalIpv4Connection() {
        var connection = OsNetworkMonitorService.parseNetstatLine(
                "TCP 192.168.1.20:55123 142.250.183.14:443 ESTABLISHED 9912")
                .orElseThrow();

        assertEquals("ESTABLISHED", connection.state());
        assertFalse(connection.loopback());
    }

    @Test
    void recognisesIpv6LoopbackAndIgnoresListeners() {
        var connection = OsNetworkMonitorService.parseNetstatLine(
                "TCP [::1]:11434 [::1]:55124 ESTABLISHED 4100")
                .orElseThrow();

        assertTrue(connection.loopback());
        assertTrue(OsNetworkMonitorService.parseNetstatLine(
                "TCP 0.0.0.0:8090 0.0.0.0:0 LISTENING 1240").isEmpty());
    }
}
