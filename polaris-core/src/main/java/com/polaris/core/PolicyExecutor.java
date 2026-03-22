package com.polaris.core;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Internal executor interface for all policies
 */
public interface PolicyExecutor<T> {
    CompletionStage<T> execute(ExecutionContext ctx, Supplier<CompletionStage<T>> next);
}