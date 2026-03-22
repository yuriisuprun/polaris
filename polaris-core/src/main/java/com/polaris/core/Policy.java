package com.polaris.core;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Core interface for a fault-tolerance policy.
 */
public interface Policy<T> {

    T execute(CheckedSupplier<T> supplier);

    CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier);

    Policy<T> and(Policy<T> next);

    Policy<T> orElse(Policy<T> fallback);

    EventPublisher events();
}