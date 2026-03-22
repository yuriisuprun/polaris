package com.polaris.config;

public class RetryConfig {

    public int attempts = 3;
    public BackoffConfig backoff;
}