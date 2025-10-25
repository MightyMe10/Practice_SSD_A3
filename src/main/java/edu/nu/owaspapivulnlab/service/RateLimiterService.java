package edu.nu.owaspapivulnlab.service;


import edu.nu.owaspapivulnlab.service.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final int LOGIN_LIMIT = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
    private static final int TRANSFER_LIMIT = 5;
    private static final Duration TRANSFER_WINDOW = Duration.ofMinutes(1);

    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    public void checkLoginByIp(String ipAddress) {
        String key = "login:ip:" + (ipAddress != null ? ipAddress : "unknown");
        recordHit(key, LOGIN_LIMIT, LOGIN_WINDOW);
    }

    public void checkLoginByUsername(String username) {
        String normalized = username == null ? "unknown" : username.toLowerCase(Locale.ROOT);
        recordHit("login:user:" + normalized, LOGIN_LIMIT, LOGIN_WINDOW);
    }

    public void checkTransferForUser(Long userId) {
        String key = "transfer:user:" + (userId != null ? userId : "anonymous");
        recordHit(key, TRANSFER_LIMIT, TRANSFER_WINDOW);
    }

    public void reset() {
        buckets.clear();
    }

    private void recordHit(String key, int limit, Duration window) {
        Deque<Instant> windowHits = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        Instant now = Instant.now();
        synchronized (windowHits) {
            Instant threshold = now.minus(window);
            while (!windowHits.isEmpty() && windowHits.peekFirst().isBefore(threshold)) {
                windowHits.pollFirst();
            }
            if (windowHits.size() >= limit) {
                throw new RateLimitExceededException("Rate limit exceeded for " + key);
            }
            windowHits.addLast(now);
        }
    }
}
