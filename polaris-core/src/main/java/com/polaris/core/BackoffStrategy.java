package com.polaris.core;

@FunctionalInterface
public interface BackoffStrategy {
    long nextDelay(int attempt);
}