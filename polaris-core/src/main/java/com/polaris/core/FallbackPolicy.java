package com.polaris.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class FallbackPolicy<T> implements Policy<T> {

    private final Supplier<T> fallback;
    private final EventPublisher events = new EventPublisher();

    public FallbackPolicy(Supplier<T> fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public T execute(CheckedSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return fallback.get();
        } catch (RuntimeException re) {
            throw re;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new PolicyExecutionException(t);
        }
    }

    @Override
    public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return CompletableFuture.completedFuture(fallback.get());
        } catch (Throwable t) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(PolarisExceptions.unwrap(t));
            return failed;
        }
    }

    @Override
    public EventPublisher events() {
        return events;
    }
}
