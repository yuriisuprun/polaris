package com.polaris.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void execute_retriesAndPublishesEvents() {
        RetryPolicy<String> policy = new RetryPolicy<>(
                3,
                attempt -> 0,
                t -> true
        );

        List<Object> events = new ArrayList<>();
        policy.events().subscribe(events::add);

        AtomicInteger calls = new AtomicInteger();
        String result = policy.execute(() -> {
            int n = calls.incrementAndGet();
            if (n <= 2) throw new IllegalStateException("boom-" + n);
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, calls.get());

        List<RetryEvent> retryEvents = events.stream()
                .filter(RetryEvent.class::isInstance)
                .map(RetryEvent.class::cast)
                .toList();
        assertEquals(2, retryEvents.size());
        assertEquals(1, retryEvents.get(0).attempt());
        assertEquals(0L, retryEvents.get(0).delayMillis());
        assertTrue(retryEvents.get(0).lastError() instanceof IllegalStateException);
        assertEquals(2, retryEvents.get(1).attempt());
    }

    @Test
    void execute_doesNotRetryWhenPredicateRejects() {
        RetryPolicy<String> policy = new RetryPolicy<>(
                3,
                attempt -> 0,
                t -> t instanceof IllegalStateException
        );

        List<Object> events = new ArrayList<>();
        policy.events().subscribe(events::add);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.execute(() -> {
                    throw new IllegalArgumentException("nope");
                }));
        assertEquals("nope", ex.getMessage());
        assertTrue(events.stream().noneMatch(RetryEvent.class::isInstance));
    }

    @Test
    void executeAsync_retriesAndEventuallySucceeds() {
        RetryPolicy<String> policy = new RetryPolicy<>(
                3,
                attempt -> 0,
                t -> true
        );

        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<String> future = policy.executeAsync(() -> {
            int n = calls.incrementAndGet();
            if (n <= 2) {
                return CompletableFuture.failedFuture(new IllegalStateException("boom-" + n));
            }
            return CompletableFuture.completedFuture("ok");
        });

        assertEquals("ok", future.join());
        assertEquals(3, calls.get());
    }

    @Test
    void executeAsync_propagatesTerminalFailure() {
        RetryPolicy<String> policy = new RetryPolicy<>(
                2,
                attempt -> 0,
                t -> true
        );

        CompletionException ex = assertThrows(CompletionException.class,
                () -> policy.executeAsync(() -> CompletableFuture.failedFuture(new IllegalStateException("boom"))).join());
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("boom", ex.getCause().getMessage());
    }
}

