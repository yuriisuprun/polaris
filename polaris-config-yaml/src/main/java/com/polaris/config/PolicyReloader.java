package com.polaris.config;

import com.polaris.core.Policy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Watches a YAML file and hot-reloads the policy.
 */
public class PolicyReloader<T> {

    private static final String THREAD_NAME = "polaris-config-watcher";

    private final Path path;
    private final Path fileName;
    private final AtomicReference<Policy<T>> current = new AtomicReference<>();

    public PolicyReloader(Path path) {
        this.path = path;
        this.fileName = path.getFileName();

        reload();
        startWatcher();
    }

    public Policy<T> get() {
        return current.get();
    }

    private void reload() {
        try (InputStream in = Files.newInputStream(path)) {
            Policy<T> policy = PolicyLoader.load(in);
            current.set(policy);
            logInfo("Policy reloaded");
        } catch (Exception e) {
            logError("Reload failed", e);
        }
    }

    private void startWatcher() {
        Thread watcherThread = new Thread(this::watchLoop, THREAD_NAME);
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            registerDirectory(watchService);

            while (true) {
                WatchKey key = watchService.take();
                processEvents(key);

                if (!key.reset()) {
                    logError("WatchKey is no longer valid, stopping watcher", null);
                    break;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Watcher thread failed", e);
        }
    }

    private void registerDirectory(WatchService watchService) throws IOException {
        Path directory = path.getParent();
        if (directory == null) {
            throw new IllegalStateException("File must have a parent directory");
        }

        directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    private void processEvents(WatchKey key) {
        for (WatchEvent<?> event : key.pollEvents()) {
            if (isTargetFileModified(event)) {
                reload();
            }
        }
    }

    private boolean isTargetFileModified(WatchEvent<?> event) {
        if (!(event.context() instanceof Path changedPath)) {
            return false;
        }
        return changedPath.endsWith(fileName);
    }

    private void logInfo(String message) {
        System.out.println("[Polaris] " + message);
    }

    private void logError(String message, Exception e) {
        if (e != null) {
            System.err.println("[Polaris] " + message + ": " + e.getMessage());
        } else {
            System.err.println("[Polaris] " + message);
        }
    }
}