package com.polaris.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerPolicyTest {

    @Test
    void opensAfterThreshold_blocksWhileOpen_thenHalfOpenProbeClosesOnSuccess() throws Exception {
        CircuitBreakerPolicy<String> policy = new CircuitBreakerPolicy<>(2, Duration.ofMillis(150));

        List<CircuitBreakerStateChangedEvent> transitions = new ArrayList<>();
        policy.events().subscribe(ev -> {
            if (ev instanceof CircuitBreakerStateChangedEvent e) transitions.add(e);
        });

        assertThrows(IllegalStateException.class, () -> policy.execute(() -> {
            throw new IllegalStateException("boom-1");
        }));
        assertThrows(IllegalStateException.class, () -> policy.execute(() -> {
            throw new IllegalStateException("boom-2");
        }));

        assertThrows(CircuitBreakerOpenException.class, () -> policy.execute(() -> "should-not-run"));

        Thread.sleep(250);

        assertEquals("ok", policy.execute(() -> "ok"));

        assertEquals(3, transitions.size());
        assertEquals(CircuitBreakerState.CLOSED, transitions.get(0).from());
        assertEquals(CircuitBreakerState.OPEN, transitions.get(0).to());
        assertEquals(CircuitBreakerState.OPEN, transitions.get(1).from());
        assertEquals(CircuitBreakerState.HALF_OPEN, transitions.get(1).to());
        assertEquals(CircuitBreakerState.HALF_OPEN, transitions.get(2).from());
        assertEquals(CircuitBreakerState.CLOSED, transitions.get(2).to());
    }
}

