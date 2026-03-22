package com.polaris.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class PolicyChain<T> implements Policy<T> {

    private final List<PolicyExecutor<T>> chain = new ArrayList<>();
    private final EventPublisher publisher = new EventPublisher();
    private final boolean fallbackOnly;

    public PolicyChain(PolicyExecutor<T> first, PolicyExecutor<T> second) {
        this(first, second, false);
    }

    public PolicyChain(PolicyExecutor<T> first, PolicyExecutor<T> second, boolean fallbackOnly) {
        chain.add(first);
        chain.add(second);
        this.fallbackOnly = fallbackOnly;
    }

    @Override
    public T execute(CheckedSupplier<T> supplier) {
        return executeAsync(() -> CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).join();
    }

    @Override
    public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> supplier) {
        Supplier<CompletionStage<T>> composed = supplier::get;

        for (int i = chain.size() - 1; i >= 0; i--) {
            PolicyExecutor<T> current = chain.get(i);
            Supplier<CompletionStage<T>> next = composed;
            composed = () -> current.execute(new ExecutionContext(), next);
        }

        return composed.get().toCompletableFuture();
    }

    @Override
    public Policy<T> and(Policy<T> next) {
        chain.add((PolicyExecutor<T>) next);
        return this;
    }

    @Override
    public Policy<T> orElse(Policy<T> fallback) {
        chain.add((PolicyExecutor<T>) fallback);
        return this;
    }

    @Override
    public EventPublisher events() {
        return publisher;
    }
}