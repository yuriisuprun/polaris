package com.polaris.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutPolicyTest {

    @Test
    void execute_timesOutAndPublishesEvent() {
        Duration timeout = Duration.ofMillis(50);
        TimeoutPolicy<String> policy = new TimeoutPolicy<>(timeout);

        List<Object> events = new ArrayList<>();
        policy.events().subscribe(events::add);

        assertThrows(PolicyTimeoutException.class, () ->
                policy.execute(() -> {
                    Thread.sleep(250);
                    return "ok";
                }));

        TimeoutEvent ev = events.stream()
                .filter(TimeoutEvent.class::isInstance)
                .map(TimeoutEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(timeout, ev.timeout());
    }

    @Test
    void executeAsync_timesOutAndPublishesEvent() {
        Duration timeout = Duration.ofMillis(30);
        TimeoutPolicy<String> policy = new TimeoutPolicy<>(timeout);

        List<Object> events = new ArrayList<>();
        policy.events().subscribe(events::add);

        CompletableFuture<String> never = new CompletableFuture<>();
        CompletionException ex = assertThrows(CompletionException.class,
                () -> policy.executeAsync(() -> never).join());
        assertTrue(ex.getCause() instanceof PolicyTimeoutException);

        TimeoutEvent ev = events.stream()
                .filter(TimeoutEvent.class::isInstance)
                .map(TimeoutEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(timeout, ev.timeout());
    }
}

