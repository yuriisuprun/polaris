package com.polaris.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class ScheduledExecutorHolder {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "polaris-scheduler");
                t.setDaemon(true);
                return t;
            }
    );

    private ScheduledExecutorHolder() { }

    public static ScheduledExecutorService scheduler() {
        return SCHEDULER;
    }
}
