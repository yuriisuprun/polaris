package com.polaris.config;

import com.polaris.core.Policy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PolicyLoaderTest {

    @Test
    void load_buildsComposablePolicyFromYaml() {
        String yaml = ""
                + "retry:\n"
                + "  attempts: 3\n"
                + "  backoff:\n"
                + "    type: fixed\n"
                + "    delay: 0\n"
                + "timeout: 2s\n";

        Policy<String> policy = PolicyLoader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        AtomicInteger calls = new AtomicInteger();
        String result = policy.execute(() -> {
            int n = calls.incrementAndGet();
            if (n <= 2) throw new IllegalStateException("boom-" + n);
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    void load_rejectsEmptyPolicy() {
        String yaml = "unknown: true\n";
        assertThrows(ConfigException.class, () ->
                PolicyLoader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    }
}

