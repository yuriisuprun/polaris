package com.polaris.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RetryPolicy<T> implements Policy<T> {

    private final int attempts;
    private final BackoffStrategy backoff;
    private final Predicate<Throwable> retryOn;
    private final EventPublisher events = new EventPublisher();

    public RetryPolicy(int attempts, BackoffStrategy backoff, Predicate<Throwable> retryOn) {
        if (attempts < 1) throw new IllegalArgumentException("attempts must be >= 1");
        this.attempts = attempts;
        this.backoff = Objects.requireNonNull(backoff, "backoff");
        this.retryOn = Objects.requireNonNull(retryOn, "retryOn");
    }

    @Override
    public T execute(CheckedSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");

        Throwable last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return supplier.get();
            } catch (Throwable t) {
                last = PolarisExceptions.unwrap(t);
                if (attempt >= attempts || !retryOn.test(last)) {
                    throw propagate(last);
                }

                long delay = Math.max(0L, backoff.nextDelay(attempt + 1));
                events.publish(new RetryEvent(attempt, delay, last));
                sleep(delay);
            }
        }

        // Should not be reachable, but keep compiler happy.
        throw propagate(last != null ? last : new IllegalStateException("retry failed"));
    }

    @Override
    public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return attemptAsync(supplier, 1, null);
    }

    private CompletableFuture<T> attemptAsync(Supplier<CompletableFuture<T>> supplier, int attempt, Throwable lastError) {
        CompletableFuture<T> current;
        try {
            current = supplier.get();
        } catch (Throwable t) {
            current = new CompletableFuture<>();
            current.completeExceptionally(PolarisExceptions.unwrap(t));
        }

        return current.handle((value, throwable) -> {
            if (throwable == null) {
                return CompletableFuture.completedFuture(value);
            }

            Throwable cause = PolarisExceptions.unwrap(throwable);
            if (attempt >= attempts || !retryOn.test(cause)) {
                CompletableFuture<T> failed = new CompletableFuture<>();
                failed.completeExceptionally(cause);
                return failed;
            }

            long delay = Math.max(0L, backoff.nextDelay(attempt + 1));
            events.publish(new RetryEvent(attempt, delay, cause));

            CompletableFuture<T> delayed = new CompletableFuture<>();
            ScheduledExecutorHolder.scheduler().schedule(
                    () -> delayed.complete(null),
                    delay,
                    java.util.concurrent.TimeUnit.MILLISECONDS
            );

            return delayed.thenCompose(ignored -> attemptAsync(supplier, attempt + 1, cause));
        }).thenCompose(f -> f);
    }

    @Override
    public EventPublisher events() {
        return events;
    }

    private static void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException re) return re;
        if (t instanceof Error e) throw e;
        return new PolicyExecutionException(t);
    }
}
