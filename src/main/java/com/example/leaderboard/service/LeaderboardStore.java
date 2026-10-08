package com.example.leaderboard.service;

import java.util.List;
import redis.clients.jedis.resps.Tuple;

/** Minimal Redis operations needed by the leaderboard. */
public interface LeaderboardStore {
    void setScore(String key, String playerId, double score);
    void incrementScore(String key, String playerId, double delta);
    Double getScore(String key, String playerId);
    Long getReverseRank(String key, String playerId);
    List<Tuple> getReverseRangeWithScores(String key, long start, long end);
    long count(String key);
    boolean remove(String key, String playerId);
}
