package com.polaris.dsl;

import com.polaris.core.BackoffStrategy;
import com.polaris.core.Policy;
import com.polaris.core.RetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class RetryBuilder {

    private final int attempts;
    private BackoffStrategy backoff = attempt -> 0;
    private Predicate<Throwable> retryOn = t -> true;
    private final List<Policy<?>> andPolicies = new ArrayList<>();

    public RetryBuilder(int attempts) {
        if (attempts < 1) throw new IllegalArgumentException("Retry attempts must be >= 1");
        this.attempts = attempts;
    }

    public RetryBuilder withBackoff(BackoffStrategy backoff) {
        this.backoff = backoff;
        return this;
    }

    public RetryBuilder retryOn(Predicate<Throwable> retryOn) {
        this.retryOn = retryOn;
        return this;
    }

    /**
     * Add an inner policy to this retry policy.
     * <p>
     * The retry policy remains the outer policy (executed first), while policies added via {@code and()}
     * are executed deeper in the chain.
     */
    public RetryBuilder and(Policy<?> next) {
        this.andPolicies.add(next);
        return this;
    }

    public <T> Policy<T> build() {
        Policy<T> policy = new RetryPolicy<>(attempts, backoff, retryOn);
        for (Policy<?> p : andPolicies) {
            policy = policy.and(cast(p));
        }
        return policy;
    }

    public <T> Policy<T> orElse(Policy<T> fallback) {
        Policy<T> built = build();
        return built.orElse(fallback);
    }

    @SuppressWarnings("unchecked")
    private static <T> Policy<T> cast(Policy<?> policy) {
        // Policies are type-agnostic in practice (they never transform the supplier output type),
        // so this cast is safe as long as implementations keep that contract.
        return (Policy<T>) policy;
    }
}
