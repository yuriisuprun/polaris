package com.polaris.core;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class PolarisExceptions {

    private PolarisExceptions() {}

    static Throwable unwrap(Throwable t) {
        if (t instanceof CompletionException ce && ce.getCause() != null) return unwrap(ce.getCause());
        if (t instanceof ExecutionException ee && ee.getCause() != null) return unwrap(ee.getCause());
        return t;
    }
}

