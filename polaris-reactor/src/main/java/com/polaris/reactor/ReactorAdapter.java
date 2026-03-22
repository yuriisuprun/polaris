package com.polaris.reactor;

import com.polaris.core.Policy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * Adapter to integrate Polaris policies with Project Reactor.
 */
public final class ReactorAdapter {

    private ReactorAdapter() { }

    /**
     * Wraps a synchronous or asynchronous call into a Mono that respects the policy.
     *
     * @param policy Polaris policy
     * @param supplier Supplier of the value
     * @param <T> Type of value
     * @return Mono emitting the value or error
     */
    public static <T> Mono<T> toMono(Policy<T> policy, Supplier<T> supplier) {
        return Mono.fromCallable(() -> policy.execute(supplier::get))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Wraps an async call (CompletableFuture) into a Mono that respects the policy.
     *
     * @param policy Polaris policy
     * @param asyncSupplier Supplier of CompletableFuture
     * @param <T> Type of value
     * @return Mono emitting the value or error
     */
    public static <T> Mono<T> toMonoAsync(Policy<T> policy, Supplier<java.util.concurrent.CompletableFuture<T>> asyncSupplier) {
        return Mono.fromFuture(() -> policy.executeAsync(asyncSupplier))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Applies a policy to a Flux stream of items.
     * Each element is executed through the policy independently.
     *
     * @param policy Polaris policy
     * @param items Flux of items
     * @param <T> Type of items
     * @return Flux with policy applied to each element
     */
    public static <T> Flux<T> toFlux(Policy<T> policy, Flux<Supplier<T>> items) {
        return items.flatMap(supplier ->
                toMono(policy, supplier)
        );
    }

    /**
     * Apply policy to a Flux of CompletableFuture items.
     *
     * @param policy Polaris policy
     * @param items Flux of async suppliers
     * @param <T> Type of items
     * @return Flux with policy applied to each element
     */
    public static <T> Flux<T> toFluxAsync(Policy<T> policy, Flux<Supplier<java.util.concurrent.CompletableFuture<T>>> items) {
        return items.flatMap(asyncSupplier ->
                toMonoAsync(policy, asyncSupplier)
        );
    }
}
