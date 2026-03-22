package com.polaris.core;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class CircuitBreakerPolicy<T> implements Policy<T> {

    private final int failureThreshold;
    private final Duration openDuration;
    private final EventPublisher events = new EventPublisher();

    private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean halfOpenProbeInFlight = new AtomicBoolean(false);
    private volatile long openUntilNanos = 0L;

    public CircuitBreakerPolicy(int failureThreshold, Duration openDuration) {
        if (failureThreshold < 1) throw new IllegalArgumentException("failureThreshold must be >= 1");
        this.failureThreshold = failureThreshold;
        this.openDuration = Objects.requireNonNull(openDuration, "openDuration");
        if (openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("openDuration must be > 0");
        }
    }

    @Override
    public T execute(CheckedSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        enterIfAllowed();
        boolean probe = state.get() == CircuitBreakerState.HALF_OPEN;
        if (probe && !halfOpenProbeInFlight.compareAndSet(false, true)) {
            throw new CircuitBreakerOpenException();
        }

        try {
            T value = supplier.get();
            onSuccess(probe);
            return value;
        } catch (Throwable t) {
            Throwable cause = PolarisExceptions.unwrap(t);
            onFailure(probe);
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error e) throw e;
            throw new PolicyExecutionException(cause);
        } finally {
            if (probe) halfOpenProbeInFlight.set(false);
        }
    }

    @Override
    public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");

        try {
            enterIfAllowed();
        } catch (RuntimeException re) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(re);
            return failed;
        }

        boolean probe = state.get() == CircuitBreakerState.HALF_OPEN;
        if (probe && !halfOpenProbeInFlight.compareAndSet(false, true)) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(new CircuitBreakerOpenException());
            return failed;
        }

        CompletableFuture<T> current;
        try {
            current = supplier.get();
        } catch (Throwable t) {
            current = new CompletableFuture<>();
            current.completeExceptionally(PolarisExceptions.unwrap(t));
        }

        return current.whenComplete((value, throwable) -> {
            try {
                if (throwable == null) {
                    onSuccess(probe);
                } else {
                    onFailure(probe);
                }
            } finally {
                if (probe) halfOpenProbeInFlight.set(false);
            }
        });
    }

    private void enterIfAllowed() {
        CircuitBreakerState current = state.get();
        if (current == CircuitBreakerState.CLOSED) return;

        if (current == CircuitBreakerState.OPEN) {
            if (System.nanoTime() < openUntilNanos) {
                throw new CircuitBreakerOpenException();
            }
            transitionTo(CircuitBreakerState.HALF_OPEN);
            return;
        }

        // HALF_OPEN: allow probe logic to decide.
    }

    private void onSuccess(boolean probe) {
        consecutiveFailures.set(0);
        if (probe) transitionTo(CircuitBreakerState.CLOSED);
    }

    private void onFailure(boolean probe) {
        if (probe) {
            open();
            return;
        }
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            open();
        }
    }

    private void open() {
        openUntilNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(openDuration.toMillis());
        transitionTo(CircuitBreakerState.OPEN);
    }

    private void transitionTo(CircuitBreakerState next) {
        CircuitBreakerState prev = state.getAndSet(next);
        if (prev != next) {
            events.publish(new CircuitBreakerStateChangedEvent(prev, next));
        }
    }

    @Override
    public EventPublisher events() {
        return events;
    }
}
