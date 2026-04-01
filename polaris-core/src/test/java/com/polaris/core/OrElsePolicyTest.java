package com.polaris.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class OrElsePolicyTest {

    @Test
    void execute_usesFallbackAndPublishesFallbackUsedEvent() {
        Policy<String> primary = new Policy<>() {
            private final EventPublisher events = new EventPublisher();

            @Override
            public String execute(CheckedSupplier<String> supplier) {
                throw new IllegalStateException("boom");
            }

            @Override
            public CompletableFuture<String> executeAsync(Supplier<CompletableFuture<String>> supplier) {
                return CompletableFuture.failedFuture(new IllegalStateException("boom"));
            }

            @Override
            public EventPublisher events() {
                return events;
            }
        };

        Policy<String> fallback = new FallbackPolicy<>(() -> "fallback");
        Policy<String> composed = primary.orElse(fallback);

        List<Object> events = new ArrayList<>();
        composed.events().subscribe(events::add);

        String result = composed.execute(() -> {
            throw new IllegalArgumentException("supplier error");
        });
        assertEquals("fallback", result);

        FallbackUsedEvent ev = events.stream()
                .filter(FallbackUsedEvent.class::isInstance)
                .map(FallbackUsedEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertTrue(ev.cause() instanceof IllegalStateException);
        assertEquals("boom", ev.cause().getMessage());
    }

    @Test
    void executeAsync_usesFallbackOnFailure() {
        Policy<String> primary = new Policy<>() {
            private final EventPublisher events = new EventPublisher();

            @Override
            public String execute(CheckedSupplier<String> supplier) {
                throw new IllegalStateException("boom");
            }

            @Override
            public CompletableFuture<String> executeAsync(Supplier<CompletableFuture<String>> supplier) {
                return CompletableFuture.failedFuture(new IllegalStateException("boom"));
            }

            @Override
            public EventPublisher events() {
                return events;
            }
        };

        Policy<String> fallback = new FallbackPolicy<>(() -> "fallback");
        Policy<String> composed = primary.orElse(fallback);

        List<Object> events = new ArrayList<>();
        composed.events().subscribe(events::add);

        assertEquals("fallback", composed.executeAsync(() -> CompletableFuture.completedFuture("ignored")).join());
        assertEquals("fallback", composed.executeAsync(() -> CompletableFuture.failedFuture(new RuntimeException("supplier-failure"))).join());

        assertTrue(events.stream().anyMatch(FallbackUsedEvent.class::isInstance));
    }
}
