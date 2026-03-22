package com.polaris.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class CircuitBreakerPolicy<T> implements PolicyExecutor<T> {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failures = new AtomicInteger(0);
    private volatile long openedAt = 0;

    public CircuitBreakerPolicy(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    @Override
    public CompletionStage<T> execute(ExecutionContext ctx, Supplier<CompletionStage<T>> next) {
        if (state.get() == State.OPEN) {
            if (System.currentTimeMillis() - openedAt > openDuration.toMillis()) {
                state.set(State.HALF_OPEN);
            } else {
                return failed(new RuntimeException("Circuit breaker is OPEN"));
            }
        }

        return next.get().whenComplete((res, err) -> {
            if (err != null) {
                int f = failures.incrementAndGet();
                if (f >= failureThreshold) {
                    state.set(State.OPEN);
                    openedAt = System.currentTimeMillis();
                }
            } else {
                failures.set(0);
                state.set(State.CLOSED);
            }
        });
    }

    private CompletableFuture<T> failed(Throwable t) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(t);
        return f;
    }
}