package com.example.leaderboard.service;

import java.util.List;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

@Repository
public class JedisRedisUseCaseStore implements RedisUseCaseStore {
    private static final String RATE_WINDOW_SCRIPT = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return {count, redis.call('TTL', KEYS[1])}
            """;
    private static final String RELEASE_LOCK_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """;

    private final JedisPooled redis;

    public JedisRedisUseCaseStore(JedisPooled redis) {
        this.redis = redis;
    }

    @Override
    public long incrementCounter(String key) {
        return redis.incr(key);
    }

    @Override
    public Long getCounter(String key) {
        String value = redis.get(key);
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public RateWindow incrementRateWindow(String key, int windowSeconds) {
        Object result = redis.eval(RATE_WINDOW_SCRIPT, List.of(key), List.of(Integer.toString(windowSeconds)));
        if (!(result instanceof List<?> values) || values.size() != 2
                || !(values.get(0) instanceof Number count) || !(values.get(1) instanceof Number ttl)) {
            throw new IllegalStateException("Unexpected Redis rate-limit script result");
        }
        return new RateWindow(count.longValue(), ttl.longValue());
    }

    @Override
    public boolean acquireLock(String key, String token, long leaseMillis) {
        return "OK".equals(redis.set(key, token, SetParams.setParams().nx().px(leaseMillis)));
    }

    @Override
    public boolean releaseLock(String key, String token) {
        Object result = redis.eval(RELEASE_LOCK_SCRIPT, List.of(key), List.of(token));
        return result instanceof Number deleted && deleted.longValue() == 1;
    }
}
