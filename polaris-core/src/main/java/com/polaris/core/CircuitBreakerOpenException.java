package com.polaris.core;

public final class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException() {
        super("Circuit breaker is OPEN");
    }
}

