package com.ngoconnect.config;

import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe in-memory sliding window rate limiter for file upload operations.
 * Enforces a maximum number of uploads within a rolling time window per client.
 */
@Component
public class UploadRateLimiter {

    private static final int MAX_UPLOADS_PER_WINDOW = 20; // 20 uploads
    private static final long WINDOW_DURATION_MS = 60_000L; // per 1 minute window

    private final Map<String, Deque<Long>> clientUploadTimestamps = new ConcurrentHashMap<>();

    public boolean tryAcquire(String clientIdentifier) {
        if (clientIdentifier == null || clientIdentifier.isBlank()) {
            clientIdentifier = "anonymous";
        }

        long now = System.currentTimeMillis();
        Deque<Long> timestamps = clientUploadTimestamps.computeIfAbsent(clientIdentifier, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst() > WINDOW_DURATION_MS)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_UPLOADS_PER_WINDOW) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }
}
