package com.teamsheet.web;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed-window counters per (bucket, key), in memory. Fine for a single machine; a restart resets them.
 * Size-bounded (least recently used keys are dropped) so a flood from many IPs can't exhaust memory.
 */
@Component
public class RateLimiter {

    static final int MAX_KEYS = 50_000;

    private final Clock clock;
    private final Map<String, long[]> windows = new LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, long[]> eldest) {
            return size() > MAX_KEYS;
        }
    };

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Counts one hit for {@code key} in {@code bucket}. Returns 0 if allowed, otherwise the seconds until the
     * current window ends (for Retry-After).
     */
    public synchronized long hit(String bucket, String key, int limit, Duration window) {
        long now = clock.millis();
        long size = window.toMillis();
        long start = now - Math.floorMod(now, size);
        long[] w = windows.computeIfAbsent(bucket + '|' + key, k -> new long[]{start, 0});
        if (w[0] != start) {
            w[0] = start;
            w[1] = 0;
        }
        if (w[1] >= limit) return Math.max(1, (start + size - now + 999) / 1000);
        w[1]++;
        return 0;
    }

    public synchronized void clear() {
        windows.clear();
    }
}
