package com.example.leaderboard.service;

import com.example.leaderboard.dto.SessionResponse;
import com.example.leaderboard.exception.SessionNotFoundException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionStore store;
    private final long ttlSeconds;

    public SessionService(
            SessionStore store,
            @Value("${redis-use-cases.session.ttl-seconds}") long ttlSeconds) {
        this.store = store;
        this.ttlSeconds = ttlSeconds;
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("Session TTL must be positive");
        }
    }

    public SessionResponse create(String userId) {
        String token = UUID.randomUUID().toString();
        store.save(token, userId, ttlSeconds);
        return new SessionResponse(token, userId, ttlSeconds);
    }

    public SessionResponse get(String token) {
        SessionStore.SessionData session = store.get(token);
        if (session == null) {
            throw new SessionNotFoundException();
        }
        return new SessionResponse(token, session.userId(), session.ttlSeconds());
    }

    public void delete(String token) {
        if (!store.delete(token)) {
            throw new SessionNotFoundException();
        }
    }
}
