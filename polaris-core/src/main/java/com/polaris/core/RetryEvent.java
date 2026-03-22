package com.polaris.core;

/**
 * Published when a retry is scheduled.
 *
 * @param attempt attempt number (1-based, where 1 is the first attempt)
 * @param delayMillis delay before the next attempt (milliseconds)
 * @param lastError the error that triggered the retry
 */
public record RetryEvent(int attempt, long delayMillis, Throwable lastError) {}

