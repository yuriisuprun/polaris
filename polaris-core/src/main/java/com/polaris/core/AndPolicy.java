package com.polaris.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class AndPolicy<T> implements Policy<T> {

    private final Policy<T> outer;
    private final Policy<T> inner;
    private final EventPublisher events = new EventPublisher();

    AndPolicy(Policy<T> outer, Policy<T> inner) {
        this.outer = Objects.requireNonNull(outer, "outer");
        this.inner = Objects.requireNonNull(inner, "inner");

        // Fan-in events from both policies into a single stream for the composed policy.
        outer.events().subscribe(events::publish);
        inner.events().subscribe(events::publish);
    }

    @Override
    public T execute(CheckedSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return outer.execute(() -> inner.execute(supplier));
    }

    @Override
    public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return outer.executeAsync(() -> inner.executeAsync(supplier));
    }

    @Override
    public EventPublisher events() {
        return events;
    }
}

