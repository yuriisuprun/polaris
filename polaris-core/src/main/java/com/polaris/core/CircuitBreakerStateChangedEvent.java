package com.polaris.core;

public record CircuitBreakerStateChangedEvent(CircuitBreakerState from, CircuitBreakerState to) {}

