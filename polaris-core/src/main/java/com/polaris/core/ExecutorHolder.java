package com.polaris.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ExecutorHolder {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "polaris-exec");
        t.setDaemon(true);
        return t;
    });

    private ExecutorHolder() {}

    static ExecutorService executor() {
        return EXECUTOR;
    }
}

