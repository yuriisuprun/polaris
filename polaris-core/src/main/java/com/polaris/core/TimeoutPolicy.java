package com.polaris.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

public final class TimeoutPolicy<T> implements PolicyExecutor<T> {

    private final Duration timeout;

    public TimeoutPolicy(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public CompletionStage<T> execute(ExecutionContext ctx, Supplier<CompletionStage<T>> next) {
        CompletableFuture<T> future = next.get().toCompletableFuture();

        ScheduledFuture<?> timeoutTask = ScheduledExecutorHolder.scheduler().schedule(() -> {
            future.completeExceptionally(new java.util.concurrent.TimeoutException());
        }, timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

        future.whenComplete((r, e) -> timeoutTask.cancel(false));

        return future;
    }
}