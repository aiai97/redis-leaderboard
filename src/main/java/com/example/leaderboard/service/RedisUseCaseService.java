package com.example.leaderboard.service;

import com.example.leaderboard.exception.LockConflictException;
import com.example.leaderboard.exception.RateLimitExceededException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RedisUseCaseService {
    private final RedisUseCaseStore store;
    private final int rateLimitRequests;
    private final int rateLimitWindowSeconds;
    private final long lockLeaseMillis;

    public RedisUseCaseService(
            RedisUseCaseStore store,
            @Value("${redis-use-cases.rate-limit.requests}") int rateLimitRequests,
            @Value("${redis-use-cases.rate-limit.window-seconds}") int rateLimitWindowSeconds,
            @Value("${redis-use-cases.lock.lease-millis}") long lockLeaseMillis) {
        this.store = store;
        this.rateLimitRequests = rateLimitRequests;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
        this.lockLeaseMillis = lockLeaseMillis;
        if (rateLimitRequests < 1 || rateLimitWindowSeconds < 1 || lockLeaseMillis < 1) {
            throw new IllegalArgumentException("Rate-limit and lock settings must be positive");
        }
    }

    public long incrementCounter(String name) {
        return store.incrementCounter("counter:" + name);
    }

    public long getCounter(String name) {
        Long count = store.getCounter("counter:" + name);
        return count == null ? 0 : count;
    }

    public RateLimitResult checkRateLimit(String clientId) {
        RedisUseCaseStore.RateWindow result =
                store.incrementRateWindow("rate-limit:" + clientId, rateLimitWindowSeconds);
        long count = result.count();
        long retryAfterSeconds = Math.max(result.ttlSeconds(), 1);
        if (count > rateLimitRequests) {
            throw new RateLimitExceededException(retryAfterSeconds);
        }
        return new RateLimitResult(rateLimitRequests - count, retryAfterSeconds);
    }

    public LockResult acquireLock(String name) {
        String token = UUID.randomUUID().toString();
        if (!store.acquireLock(lockKey(name), token, lockLeaseMillis)) {
            throw new LockConflictException("Lock is already held");
        }
        return new LockResult(token, lockLeaseMillis);
    }

    public void releaseLock(String name, String token) {
        if (!store.releaseLock(lockKey(name), token)) {
            throw new LockConflictException("Lock is not held by this token");
        }
    }

    private String lockKey(String name) {
        return "lock:" + name;
    }

    public record RateLimitResult(long remaining, long retryAfterSeconds) {}
    public record LockResult(String token, long leaseMillis) {}
}
