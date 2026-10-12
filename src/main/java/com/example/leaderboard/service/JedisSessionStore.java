package com.example.leaderboard.service;

import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;
import java.util.List;

@Repository
public class JedisSessionStore implements SessionStore {
    private static final String GET_SESSION_SCRIPT = """
            local userId = redis.call('GET', KEYS[1])
            if not userId then
                return nil
            end
            return {userId, redis.call('PTTL', KEYS[1])}
            """;
    private final JedisPooled redis;

    public JedisSessionStore(JedisPooled redis) {
        this.redis = redis;
    }

    @Override
    public void save(String token, String userId, long ttlSeconds) {
        redis.set(key(token), userId, SetParams.setParams().ex(ttlSeconds));
    }

    @Override
    public SessionData get(String token) {
        Object result = redis.eval(GET_SESSION_SCRIPT, List.of(key(token)), List.of());
        if (result == null) {
            return null;
        }
        if (!(result instanceof List<?> values) || values.size() != 2
                || !(values.get(0) instanceof String userId)
                || !(values.get(1) instanceof Number ttlMillis)) {
            throw new IllegalStateException("Unexpected Redis session script result");
        }
        long ttlSeconds = Math.max(0, (ttlMillis.longValue() + 999) / 1000);
        return new SessionData(userId, ttlSeconds);
    }

    @Override
    public boolean delete(String token) {
        return redis.del(key(token)) == 1;
    }

    private String key(String token) {
        return "session:" + token;
    }
}
