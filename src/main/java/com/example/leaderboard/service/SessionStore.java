package com.example.leaderboard.service;

public interface SessionStore {
    void save(String token, String userId, long ttlSeconds);
    SessionData get(String token);
    boolean delete(String token);

    record SessionData(String userId, long ttlSeconds) {}
}
