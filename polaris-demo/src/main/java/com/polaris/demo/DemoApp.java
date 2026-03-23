package com.polaris.demo;

import com.polaris.dsl.DSL;
import com.polaris.core.Policy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;


public class DemoApp {

    public static void main(String[] args) {

        // ====== Build a composite policy ======
        Policy<String> policy = DSL.retry(3)
                .withBackoff(DSL.exponential(100))
                .and(DSL.timeout(Duration.ofSeconds(2)))
                .and(DSL.circuitBreaker(5, Duration.ofSeconds(10)))
                .orElse(DSL.fallback(() -> "fallback-value"));

        // ====== Synchronous execution ======
        try {
            String result = policy.execute(DemoApp::unreliableCall);
            System.out.println("Sync result: " + result);
        } catch (Exception e) {
            System.err.println("Sync call failed: " + e.getMessage());
        }

        // ====== Asynchronous execution ======
        CompletableFuture<String> futureResult = policy.executeAsync(() ->
                CompletableFuture.supplyAsync(DemoApp::unreliableCall)
        );

        futureResult.whenComplete((res, err) -> {
            if (err != null) {
                System.err.println("Async call failed: " + err.getMessage());
            } else {
                System.out.println("Async result: " + res);
            }
        }).join();

        // ====== Subscribe to retry events ======
        policy.events().subscribe(event -> System.out.println("Event: " + event));
    }

    // Simulated unreliable service call
    private static String unreliableCall() {
        if (Math.random() < 0.7) {
            throw new RuntimeException("Simulated failure");
        }
        return "Success!";
    }
}
