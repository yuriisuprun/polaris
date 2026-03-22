package com.polaris.dsl;

import com.polaris.core.*;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Fluent DSL for building Polaris fault-tolerance policies.
 */
public final class DSL {

    private DSL() { }

    // ====== Retry ======
    public static <T> RetryBuilder<T> retry(int attempts) {
        return new RetryBuilder<>(attempts);
    }

    // ====== Timeout ======
    public static <T> Policy<T> timeout(Duration duration) {
        return new TimeoutPolicy<>(duration);
    }

    // ====== Circuit Breaker ======
    public static <T> Policy<T> circuitBreaker(int failureThreshold, Duration openDuration) {
        return new CircuitBreakerPolicy<>(failureThreshold, openDuration);
    }

    // ====== Fallback ======
    public static <T> Policy<T> fallback(Supplier<T> fallback) {
        return new FallbackPolicy<>(fallback);
    }

    // ====== Backoff Strategies ======
    public static BackoffStrategy fixed(long millis) {
        return attempt -> millis;
    }

    public static BackoffStrategy exponential(long baseMillis) {
        return new ExponentialBackoff(baseMillis);
    }
}