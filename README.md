🌌 Polaris – composable fault-tolerance DSL library for Java

Overview

Polaris is a modern, fluent, and composable fault-tolerance library for Java.

It enables developers to define retries, timeouts, circuit breakers, and fallbacks with a minimal, readable DSL. Unlike traditional libraries like Resilience4j, Polaris focuses on:

Fluent composition with and() / orElse()
Unified execution for synchronous, asynchronous, and reactive code
Immutable and thread-safe policies
Observability and metrics integration (Micrometer)
YAML-based configuration and hot reload support
Features
Retry policies with max attempts, exception filtering, and backoff strategies (fixed/exponential)
Timeout policies with hard enforcement and cancellation
Circuit breakers with failure threshold, sliding window metrics, and state transitions
Fallback policies (static or dynamic)
Unified sync / async / reactive execution

DSL for composability:

Policy<String> policy = retry(3)
    .withBackoff(exponential(100))
    .and(timeout(Duration.ofSeconds(2)))
    .and(circuitBreaker(50, Duration.ofSeconds(10)))
    .orElse(fallback(() -> "default"));
Metrics: Retry count, failure rate, circuit state
Events: Subscribe to policy lifecycle events
YAML-based configuration with optional hot reload
Quick Start

Add Polaris to your Maven project:

<dependency>
    <groupId>com.polaris</groupId>
    <artifactId>polaris-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.polaris</groupId>
    <artifactId>polaris-dsl</artifactId>
    <version>1.0.0</version>
</dependency>
Basic Example
import com.polaris.dsl.DSL.*;
import java.time.Duration;

public class Example {
    public static void main(String[] args) {
        Policy<String> policy = retry(3)
            .withBackoff(exponential(100))
            .and(timeout(Duration.ofSeconds(2)))
            .and(circuitBreaker(5, Duration.ofSeconds(10)))
            .orElse(fallback(() -> "fallback-value"));

        String result = policy.execute(() -> httpClient.get("https://api.example.com"));
        System.out.println(result);
    }
}
Async / Reactive Example
CompletableFuture<String> future = policy.executeAsync(() ->
    CompletableFuture.supplyAsync(() -> httpClient.get("https://api.example.com"))
);

Mono<String> mono = ReactorAdapter.toMono(policy, () -> httpClient.getReactive("https://api.example.com"));
YAML Configuration Example
policy:
  retry:
    attempts: 5
    backoff: exponential
  timeout: 2s
  circuitBreaker:
    failureThreshold: 50
    openDuration: 10s

Load in Java:

Policy<String> policy = PolicyLoader.load(new FileInputStream("policies.yaml"));
Observability

Subscribe to policy events:

policy.events().subscribe(event -> {
    if (event instanceof RetryEvent retry) {
        System.out.println("Retry attempt: " + retry.attempt());
    }
});

Metrics:

polaris.retry.count – Retry count
polaris.circuit.state – Circuit breaker state
polaris.execution.time – Execution duration
Comparison with Resilience4j
Feature	Polaris	Resilience4j
DSL	Fluent, composable	Builder-heavy
Sync/Async	Unified	Separate APIs
Circuit breaker	Full state machine	Yes
Retry	Exception filtering + backoff	Yes
Reactive	Reactor support	Limited
YAML Config	Yes, hot reload	Partial
Why Polaris Exists

Polaris was created to reduce boilerplate and increase readability while still offering powerful fault-tolerance features.

Developers no longer need separate builders for each policy
Supports modern reactive programming
Fully composable policies for microservices and async systems
Advanced Features
Simulation Mode – Test policies without executing real calls
Debug Tracing – Print detailed decision logs
Visualization Hooks – Export policy execution graphs
Contributing
Fork the repository
Create a branch for your feature (git checkout -b feature/my-feature)
Commit changes (git commit -m 'Add feature')
Push (git push origin feature/my-feature)
Open a pull request
License

Apache License 2.0
