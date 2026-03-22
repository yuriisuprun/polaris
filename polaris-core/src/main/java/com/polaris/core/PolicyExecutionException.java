package com.polaris.core;

/**
 * Wraps a checked exception thrown by a user supplier.
 */
public final class PolicyExecutionException extends RuntimeException {
    public PolicyExecutionException(Throwable cause) {
        super(cause);
    }
}

