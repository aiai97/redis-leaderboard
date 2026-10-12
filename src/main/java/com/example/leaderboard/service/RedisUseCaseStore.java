package com.example.leaderboard.service;

public interface RedisUseCaseStore {
    long incrementCounter(String key);
    Long getCounter(String key);
    RateWindow incrementRateWindow(String key, int windowSeconds);
    boolean acquireLock(String key, String token, long leaseMillis);
    boolean releaseLock(String key, String token);

    record RateWindow(long count, long ttlSeconds) {}
}
