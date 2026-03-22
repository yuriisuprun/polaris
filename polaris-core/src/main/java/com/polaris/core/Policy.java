package com.polaris.core;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Core interface for a fault-tolerance policy.
 */
public interface Policy<T> {

    T execute(CheckedSupplier<T> supplier);

    CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier);

    /**
     * Compose this policy with a next policy.
     * <p>
     * Composition is left-to-right and results in this policy being the outer policy:
     * {@code retry.and(timeout)} means timeout is executed inside each retry attempt.
     */
    default Policy<T> and(Policy<T> next) {
        return new AndPolicy<>(this, next);
    }

    /**
     * Provide a fallback policy executed when this policy fails.
     */
    default Policy<T> orElse(Policy<T> fallback) {
        return new OrElsePolicy<>(this, fallback);
    }

    EventPublisher events();
}
