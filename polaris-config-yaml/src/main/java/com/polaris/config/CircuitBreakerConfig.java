package com.polaris.config;

public class CircuitBreakerConfig {

    public int failureThreshold = 5;
    public String openDuration = "10s";
}