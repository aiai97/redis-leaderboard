package com.example.leaderboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.leaderboard.exception.SessionNotFoundException;
import org.junit.jupiter.api.Test;

class SessionServiceTest {
    @Test
    void createsLooksUpAndDeletesExpiringSession() {
        FakeStore store = new FakeStore();
        SessionService service = new SessionService(store, 1800);

        var created = service.create("user-42");
        assertThat(created.userId()).isEqualTo("user-42");
        assertThat(created.expiresInSeconds()).isEqualTo(1800);
        assertThat(store.savedTtl).isEqualTo(1800);

        store.session = new SessionStore.SessionData("user-42", 1799);
        assertThat(service.get(created.token()).expiresInSeconds()).isEqualTo(1799);
        service.delete(created.token());
        assertThatThrownBy(() -> service.get(created.token())).isInstanceOf(SessionNotFoundException.class);
    }

    private static class FakeStore implements SessionStore {
        private SessionData session;
        private long savedTtl;

        @Override
        public void save(String token, String userId, long ttlSeconds) {
            savedTtl = ttlSeconds;
            session = new SessionData(userId, ttlSeconds);
        }

        @Override public SessionData get(String token) { return session; }

        @Override
        public boolean delete(String token) {
            boolean existed = session != null;
            session = null;
            return existed;
        }
    }
}
