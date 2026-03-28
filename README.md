# 🌌 Polaris

**Composable Fault-Tolerance DSL for Modern Java**

---

## Overview

**Polaris** is a modern, fluent, and composable fault-tolerance library for Java designed for high-performance, distributed, and reactive systems.

It provides a **concise DSL** to declaratively compose resilience strategies such as retries, timeouts, circuit breakers, and fallbacks—without the verbosity and fragmentation of traditional approaches.

Polaris is built around a few core principles:

* **Fluent composition over configuration-heavy builders**
* **Unified execution model (sync, async, reactive)**
* **Immutable, thread-safe policy definitions**
* **First-class observability and metrics**
* **Externalized configuration with hot reload**

---

## Key Features

### Composable DSL

Build complex fault-tolerance strategies with readable, chainable operators:

```java
Policy<String> policy = retry(3)
    .withBackoff(exponential(100))
    .and(timeout(Duration.ofSeconds(2)))
    .and(circuitBreaker(50, Duration.ofSeconds(10)))
    .orElse(fallback(() -> "default"));
```

---

### Retry Policies

* Max attempts
* Exception filtering
* Backoff strategies:

    * Fixed
    * Exponential
    * Custom

---

### Timeout Control

* Hard time limits
* Cancellation support
* Works across sync and async executions

---

### Circuit Breaker

* Sliding window metrics
* Failure thresholds
* Full state machine:

    * Closed → Open → Half-Open

---

### Fallbacks

* Static fallback values
* Dynamic fallback functions
* Context-aware fallback handling

---

### Unified Execution Model

Execute the same policy across:

* Synchronous code
* `CompletableFuture`
* Reactive streams (Project Reactor)

---

### Observability & Metrics

* Built-in event system
* Micrometer integration
* Low-overhead instrumentation

---

### Configuration

* YAML-based policy definitions
* Environment-driven overrides

---

## Installation

### Maven

```xml
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
```

---

## Quick Start

```java
import static com.polaris.dsl.DSL.*;
import java.time.Duration;

public class Example {
    public static void main(String[] args) {
        Policy<String> policy = retry(3)
            .withBackoff(exponential(100))
            .and(timeout(Duration.ofSeconds(2)))
            .and(circuitBreaker(5, Duration.ofSeconds(10)))
            .orElse(fallback(() -> "fallback-value"));

        String result = policy.execute(() ->
            httpClient.get("https://api.example.com")
        );

        System.out.println(result);
    }
}
```

---

## Async & Reactive Usage

### CompletableFuture

```java
CompletableFuture<String> future = policy.executeAsync(() ->
    CompletableFuture.supplyAsync(() ->
        httpClient.get("https://api.example.com")
    )
);
```

### Reactor

```java
Mono<String> mono = ReactorAdapter.toMono(
    policy,
    () -> httpClient.getReactive("https://api.example.com")
);
```

---

## YAML Configuration

Define policies declaratively:

```yaml
policy:
  retry:
    attempts: 5
    backoff: exponential
  timeout: 2s
  circuitBreaker:
    failureThreshold: 50
    openDuration: 10s
```

Load at runtime:

```java
Policy<String> policy =
    PolicyLoader.load(new FileInputStream("policies.yaml"));
```

---

## Observability

### Event Subscription

```java
policy.events().subscribe(event -> {
    if (event instanceof RetryEvent retry) {
        System.out.println("Retry attempt: " + retry.attempt());
    }
});
```

---

### Metrics

| Metric                   | Description           |
| ------------------------ | --------------------- |
| `polaris.retry.count`    | Total retry attempts  |
| `polaris.circuit.state`  | Circuit breaker state |
| `polaris.execution.time` | Execution latency     |

---

## Comparison

| Feature          | Polaris                      | Traditional Libraries |
| ---------------- | ---------------------------- | --------------------- |
| DSL              | Fluent, composable           | Builder-heavy         |
| Sync / Async     | Unified                      | Separate APIs         |
| Circuit Breaker  | Full state machine           | Yes                   |
| Retry            | Advanced filtering + backoff | Yes                   |
| Reactive Support | Native                       | Limited               |
| YAML Config      | Yes (hot reload)             | Partial               |

---

## Why Polaris

Polaris exists to eliminate the friction between **resilience and readability**.

It enables:

* Declarative fault-tolerance
* Reduced boilerplate
* Consistent execution across paradigms
* Clean integration into microservices and reactive systems

Instead of stitching together multiple components, Polaris lets you define **a single, composable policy pipeline**.

---

## Advanced Features

* **Simulation Mode**
  Validate policies without executing real calls

* **Debug Tracing**
  Inspect decision flow and policy behavior

* **Visualization Hooks**
  Export execution graphs for analysis and tooling

---

## Contributing

1. Fork the repository
2. Create your feature branch

   ```bash
   git checkout -b feature/my-feature
   ```
3. Commit your changes

   ```bash
   git commit -m "Add feature"
   ```
4. Push to your branch

   ```bash
   git push origin feature/my-feature
   ```
5. Open a Pull Request

---

## License

Licensed under the **Apache License 2.0**.

---

## Roadmap

* Kotlin DSL support
* Spring Boot starter
* Distributed tracing integration
* Policy composition visualization UI

---

**Polaris brings clarity to resilience.**
