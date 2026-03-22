package com.polaris.core;

import java.time.Duration;

public final class PolicyTimeoutException extends RuntimeException {
    public PolicyTimeoutException(Duration timeout) {
        super("Execution timed out after " + timeout);
    }
}

