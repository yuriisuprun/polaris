package com.polaris.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RetryPolicy<T> implements PolicyExecutor<T> {

    private final int maxAttempts;
    private final BackoffStrategy backoff;
    private final Predicate<Throwable> retryOn;

    public RetryPolicy(int maxAttempts, BackoffStrategy backoff, Predicate<Throwable> retryOn) {
        this.maxAttempts = maxAttempts;
        this.backoff = backoff;
        this.retryOn = retryOn;
    }

    @Override
    public CompletionStage<T> execute(ExecutionContext ctx, Supplier<CompletionStage<T>> next) {
        CompletableFuture<T> result = new CompletableFuture<>();
        attempt(1, result, ctx, next);
        return result;
    }

    private void attempt(int attempt, CompletableFuture<T> result,
                         ExecutionContext ctx, Supplier<CompletionStage<T>> next) {
        next.get().whenComplete((value, error) -> {
            if (error == null) {
                result.complete(value);
                return;
            }

            Throwable cause = unwrap(error);

            if (attempt >= maxAttempts || !retryOn.test(cause)) {
                result.completeExceptionally(cause);
                return;
            }

            long delay = backoff.nextDelay(attempt);

            ScheduledFuture<?> scheduled = ScheduledExecutorHolder.scheduler().schedule(
                    () -> attempt(attempt + 1, result, ctx, next),
                    delay, TimeUnit.MILLISECONDS
            );
        });
    }

    private Throwable unwrap(Throwable t) {
        if (t instanceof java.util.concurrent.CompletionException ce) {
            return ce.getCause();
        }
        return t;
    }
}