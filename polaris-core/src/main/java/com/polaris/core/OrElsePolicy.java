package com.polaris.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

final class OrElsePolicy<T> implements Policy<T> {

    private final Policy<T> primary;
    private final Policy<T> fallback;
    private final EventPublisher events = new EventPublisher();

    OrElsePolicy(Policy<T> primary, Policy<T> fallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");

        primary.events().subscribe(events::publish);
        fallback.events().subscribe(events::publish);
    }

    @Override
    public T execute(CheckedSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return primary.execute(supplier);
        } catch (Throwable t) {
            events.publish(new FallbackUsedEvent(PolarisExceptions.unwrap(t)));
            return fallback.execute(supplier);
        }
    }

    @Override
    public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return primary.executeAsync(supplier).handle((value, throwable) -> {
            if (throwable == null) {
                return CompletableFuture.completedFuture(value);
            }
            Throwable cause = PolarisExceptions.unwrap(throwable);
            events.publish(new FallbackUsedEvent(cause));
            try {
                return fallback.executeAsync(supplier);
            } catch (Throwable t) {
                CompletableFuture<T> failed = new CompletableFuture<>();
                failed.completeExceptionally(PolarisExceptions.unwrap(t));
                return failed;
            }
        }).thenCompose(f -> f);
    }

    @Override
    public EventPublisher events() {
        return events;
    }
}

