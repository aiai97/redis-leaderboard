package com.example.leaderboard.service;

import java.util.List;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.Tuple;

@Repository
public class JedisLeaderboardStore implements LeaderboardStore {
    private final JedisPooled redis;

    public JedisLeaderboardStore(JedisPooled redis) {
        this.redis = redis;
    }

    @Override public void setScore(String key, String playerId, double score) { redis.zadd(key, score, playerId); }
    @Override public void incrementScore(String key, String playerId, double delta) { redis.zincrby(key, delta, playerId); }
    @Override public Double getScore(String key, String playerId) { return redis.zscore(key, playerId); }
    @Override public Long getReverseRank(String key, String playerId) { return redis.zrevrank(key, playerId); }
    @Override public List<Tuple> getReverseRangeWithScores(String key, long start, long end) {
        return redis.zrevrangeWithScores(key, start, end);
    }
    @Override public long count(String key) { return redis.zcard(key); }
    @Override public boolean remove(String key, String playerId) { return redis.zrem(key, playerId) > 0; }
}
