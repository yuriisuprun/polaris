package com.polaris.demo;

import com.polaris.dsl.DSL;
import com.polaris.core.Policy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DemoApp {

    private static final int RETRY_COUNT = 3;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CIRCUIT_BREAKER_DURATION = Duration.ofSeconds(10);

    private static final int BACKOFF_INITIAL_DELAY_MS = 100;
    private static final String FALLBACK_VALUE = "fallback-value";

    public static void main(String[] args) {
        Policy<String> policy = buildPolicy();

        executeSync(policy);
        executeAsync(policy);
        subscribeToEvents(policy);
    }

    private static Policy<String> buildPolicy() {
        return DSL.retry(RETRY_COUNT)
                .withBackoff(DSL.exponential(BACKOFF_INITIAL_DELAY_MS))
                .and(DSL.timeout(TIMEOUT))
                .and(DSL.circuitBreaker(CIRCUIT_BREAKER_THRESHOLD, CIRCUIT_BREAKER_DURATION))
                .orElse(DSL.fallback(() -> FALLBACK_VALUE));
    }

    private static void executeSync(Policy<String> policy) {
        try {
            String result = policy.execute(DemoApp::unreliableCall);
            logSuccess("Sync", result);
        } catch (Exception e) {
            logError("Sync", e);
        }
    }

    private static void executeAsync(Policy<String> policy) {
        CompletableFuture<String> future = policy.executeAsync(DemoApp::asyncUnreliableCall);

        future.whenComplete((result, error) -> {
            if (error != null) {
                logError("Async", error);
            } else {
                logSuccess("Async", result);
            }
        }).join();
    }

    private static CompletableFuture<String> asyncUnreliableCall() {
        return CompletableFuture.supplyAsync(DemoApp::unreliableCall);
    }

    private static void subscribeToEvents(Policy<String> policy) {
        policy.events().subscribe(event -> System.out.println("Event: " + event));
    }

    private static void logSuccess(String context, String result) {
        System.out.println(context + " result: " + result);
    }

    private static void logError(String context, Throwable error) {
        System.err.println(context + " call failed: " + error.getMessage());
    }

    // Simulated unreliable service call
    private static String unreliableCall() {
        if (Math.random() < 0.7) {
            throw new RuntimeException("Simulated failure");
        }
        return "Success!";
    }
}