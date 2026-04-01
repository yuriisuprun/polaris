package com.polaris.reactor;

import com.polaris.core.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReactorAdapterTest {

    @Test
    void toMono_runsThroughPolicy() {
        RetryPolicy<String> policy = new RetryPolicy<>(2, attempt -> 0, t -> true);

        AtomicInteger calls = new AtomicInteger();
        String value = ReactorAdapter.toMono(policy, () -> {
            int n = calls.incrementAndGet();
            if (n == 1) throw new IllegalStateException("boom");
            return "ok";
        }).block(Duration.ofSeconds(2));

        assertEquals("ok", value);
        assertEquals(2, calls.get());
    }

    @Test
    void toMonoAsync_runsThroughPolicy() {
        RetryPolicy<String> policy = new RetryPolicy<>(2, attempt -> 0, t -> true);

        AtomicInteger calls = new AtomicInteger();
        String value = ReactorAdapter.toMonoAsync(policy, () -> {
            int n = calls.incrementAndGet();
            if (n == 1) return CompletableFuture.failedFuture(new IllegalStateException("boom"));
            return CompletableFuture.completedFuture("ok");
        }).block(Duration.ofSeconds(2));

        assertEquals("ok", value);
        assertEquals(2, calls.get());
    }
}

