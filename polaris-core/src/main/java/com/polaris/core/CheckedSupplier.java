package com.polaris.core;

@FunctionalInterface
public interface CheckedSupplier<T> {
    T get() throws Exception;
}
