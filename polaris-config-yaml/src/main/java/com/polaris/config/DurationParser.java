package com.polaris.config;

import java.time.Duration;

/**
 * Parses human-readable durations like:
 * 100ms, 2s, 5m
 */
public final class DurationParser {

    private DurationParser() {}

    public static Duration parse(String value) {
        if (value == null) return null;

        value = value.trim().toLowerCase();

        if (value.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(value.replace("ms", "")));
        }
        if (value.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(value.replace("s", "")));
        }
        if (value.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(value.replace("m", "")));
        }

        throw new ConfigException("Invalid duration: " + value);
    }
}