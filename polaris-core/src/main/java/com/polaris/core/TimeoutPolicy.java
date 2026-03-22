package com.polaris.core;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class TimeoutPolicy<T> implements Policy<T> {

    private final Duration timeout;
    private final EventPublisher events = new EventPublisher();

    public TimeoutPolicy(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be > 0");
        }
    }

    @Override
    public T execute(CheckedSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");

        Future<T> future = ExecutorHolder.executor().submit(() -> supplier.get());
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException te) {
            future.cancel(true);
            events.publish(new TimeoutEvent(timeout));
            throw new PolicyTimeoutException(timeout);
        } catch (java.util.concurrent.ExecutionException ee) {
            Throwable cause = PolarisExceptions.unwrap(ee.getCause() != null ? ee.getCause() : ee);
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error e) throw e;
            throw new PolicyExecutionException(cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new PolicyExecutionException(ie);
        }
    }

    @Override
    public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");

        CompletableFuture<T> original;
        try {
            original = supplier.get();
        } catch (Throwable t) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(PolarisExceptions.unwrap(t));
            return failed;
        }

        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        ScheduledExecutorHolder.scheduler().schedule(
                () -> {
                    events.publish(new TimeoutEvent(timeout));
                    timeoutFuture.completeExceptionally(new PolicyTimeoutException(timeout));
                    original.cancel(true);
                },
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        return original.applyToEither(timeoutFuture, v -> v);
    }

    @Override
    public EventPublisher events() {
        return events;
    }
}
