package com.polaris.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {

    @Test
    void parse_supportsMsSecondsMinutes() {
        assertEquals(Duration.ofMillis(100), DurationParser.parse("100ms"));
        assertEquals(Duration.ofSeconds(2), DurationParser.parse("2s"));
        assertEquals(Duration.ofMinutes(5), DurationParser.parse("5m"));
    }

    @Test
    void parse_trimsAndIsCaseInsensitive() {
        assertEquals(Duration.ofMillis(10), DurationParser.parse(" 10MS "));
    }

    @Test
    void parse_rejectsUnknownUnits() {
        assertThrows(ConfigException.class, () -> DurationParser.parse("1h"));
    }
}

