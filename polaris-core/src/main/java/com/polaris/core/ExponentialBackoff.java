package com.polaris.core;

public final class ExponentialBackoff implements BackoffStrategy {

    private final long baseMillis;

    public ExponentialBackoff(long baseMillis) {
        this.baseMillis = baseMillis;
    }

    @Override
    public long nextDelay(int attempt) {
        return (long) (baseMillis * Math.pow(2, attempt - 1));
    }
}