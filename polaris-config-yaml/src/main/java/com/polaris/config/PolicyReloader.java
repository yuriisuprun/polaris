package com.polaris.config;

import com.polaris.core.Policy;

import java.io.FileInputStream;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Watches a YAML file and hot-reloads the policy.
 */
public class PolicyReloader<T> {

    private final Path path;
    private final AtomicReference<Policy<T>> current = new AtomicReference<>();

    public PolicyReloader(Path path) {
        this.path = path;
        reload();
        watch();
    }

    public Policy<T> get() {
        return current.get();
    }

    private void reload() {
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            current.set(PolicyLoader.load(in));
            System.out.println("[Polaris] Policy reloaded");
        } catch (Exception e) {
            System.err.println("[Polaris] Reload failed: " + e.getMessage());
        }
    }

    private void watch() {
        new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();

                path.getParent().register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );

                while (true) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();

                        if (changed.endsWith(path.getFileName())) {
                            reload();
                        }
                    }

                    key.reset();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "polaris-config-watcher").start();
    }
}