package com.polaris.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventPublisher {
    private final List<Consumer<Object>> subscribers = new CopyOnWriteArrayList<>();

    public void publish(Object event) {
        subscribers.forEach(s -> s.accept(event));
    }

    public void subscribe(Consumer<Object> consumer) {
        subscribers.add(consumer);
    }
}