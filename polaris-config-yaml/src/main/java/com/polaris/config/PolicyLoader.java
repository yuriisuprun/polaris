package com.polaris.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.polaris.core.Policy;
import com.polaris.dsl.DSL;

import java.io.InputStream;
import java.time.Duration;

/**
 * Loads YAML config and converts it into a Polaris Policy.
 */
public final class PolicyLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private PolicyLoader() {}

    public static <T> Policy<T> load(InputStream yaml) {
        try {
            PolicyConfig config = MAPPER.readValue(yaml, PolicyConfig.class);

            Policy<T> policy = null;

            // ===== Retry =====
            if (config.retry != null) {
                var retryBuilder = DSL.<T>retry(config.retry.attempts);

                if (config.retry.backoff != null) {
                    retryBuilder.withBackoff(resolveBackoff(config.retry.backoff));
                }

                policy = retryBuilder.build();
            }

            // ===== Timeout =====
            if (config.timeout != null) {
                Duration timeout = DurationParser.parse(config.timeout);
                policy = combine(policy, DSL.timeout(timeout));
            }

            // ===== Circuit Breaker =====
            if (config.circuitBreaker != null) {
                Duration openDuration = DurationParser.parse(config.circuitBreaker.openDuration);

                policy = combine(policy,
                        DSL.circuitBreaker(
                                config.circuitBreaker.failureThreshold,
                                openDuration
                        ));
            }

            if (policy == null) {
                throw new ConfigException("No policy defined in YAML");
            }

            return policy;

        } catch (Exception e) {
            throw new ConfigException("Failed to load policy config", e);
        }
    }

    private static <T> Policy<T> combine(Policy<T> current, Policy<T> next) {
        if (current == null) return next;
        return current.and(next);
    }

    private static com.polaris.core.BackoffStrategy resolveBackoff(BackoffConfig cfg) {
        return switch (cfg.type.toLowerCase()) {
            case "exponential" -> DSL.exponential(cfg.delay);
            case "fixed" -> DSL.fixed(cfg.delay);
            default -> throw new ConfigException("Unknown backoff type: " + cfg.type);
        };
    }
}