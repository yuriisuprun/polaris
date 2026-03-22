package com.polaris.core;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public final class FallbackPolicy<T> implements PolicyExecutor<T> {

    private final Supplier<T> fallback;

    public FallbackPolicy(Supplier<T> fallback) {
        this.fallback = fallback;
    }

    @Override
    public CompletionStage<T> execute(ExecutionContext ctx, Supplier<CompletionStage<T>> next) {
        return next.get().exceptionally(ex -> fallback.get());
    }
}