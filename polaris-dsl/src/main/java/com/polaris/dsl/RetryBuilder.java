package com.polaris.dsl;

import com.polaris.core.*;

import java.util.function.Predicate;

/**
 * Builder class for fluent retry configuration.
 */
public final class RetryBuilder<T> {

    private final int attempts;
    private BackoffStrategy backoff = attempt -> 0;
    private Predicate<Throwable> retryOn = t -> true;

    public RetryBuilder(int attempts) {
        if (attempts < 1) throw new IllegalArgumentException("Retry attempts must be >= 1");
        this.attempts = attempts;
    }

    /** Set backoff strategy */
    public RetryBuilder<T> withBackoff(BackoffStrategy backoff) {
        this.backoff = backoff;
        return this;
    }

    /** Set exception filter */
    public RetryBuilder<T> retryOn(Predicate<Throwable> retryOn) {
        this.retryOn = retryOn;
        return this;
    }

    /** Build RetryPolicy */
    public Policy<T> build() {
        return new RetryPolicy<>(attempts, backoff, retryOn);
    }

    /** Compose with another policy */
    public Policy<T> and(Policy<T> next) {
        return new PolicyChain<>(build(), (PolicyExecutor<T>) next);
    }

    /** Fallback policy on failure */
    public Policy<T> orElse(Policy<T> fallback) {
        return new PolicyChain<>(build(), (PolicyExecutor<T>) fallback, true);
    }
}