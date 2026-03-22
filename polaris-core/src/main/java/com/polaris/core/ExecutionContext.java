package com.polaris.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ExecutionContext {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public <T> void put(String key, T value) {
        attributes.put(key, value);
    }

    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) attributes.get(key));
    }
}