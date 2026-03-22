package com.polaris.dsl;

import com.polaris.core.*;

import java.time.Duration;
import java.util.function.Supplier;

public final class DSL {

    private DSL() {}

    public static RetryBuilder retry(int attempts) {
        return new RetryBuilder(attempts);
    }

    public static <T> Policy<T> timeout(Duration duration) {
        return new TimeoutPolicy<>(duration);
    }

    public static <T> Policy<T> circuitBreaker(int failureThreshold, Duration openDuration) {
        return new CircuitBreakerPolicy<>(failureThreshold, openDuration);
    }

    public static <T> Policy<T> fallback(Supplier<T> fallback) {
        return new FallbackPolicy<>(fallback);
    }

    public static BackoffStrategy fixed(long millis) {
        return attempt -> millis;
    }

    public static BackoffStrategy exponential(long baseMillis) {
        return new ExponentialBackoff(baseMillis);
    }
}
