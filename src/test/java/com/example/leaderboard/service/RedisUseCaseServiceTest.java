package com.example.leaderboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.leaderboard.exception.LockConflictException;
import com.example.leaderboard.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

class RedisUseCaseServiceTest {
    @Test
    void counterUsesNamespacedKeyAndTreatsMissingCounterAsZero() {
        FakeStore store = new FakeStore();
        RedisUseCaseService service = service(store);

        assertThat(service.getCounter("article:42:views")).isZero();
        assertThat(service.incrementCounter("article:42:views")).isEqualTo(1);
        assertThat(store.lastKey).isEqualTo("counter:article:42:views");
    }

    @Test
    void rateLimitAllowsRequestsUnderLimitAndRejectsExcessRequests() {
        FakeStore store = new FakeStore();
        RedisUseCaseService service = service(store);

        store.rateResult = new RedisUseCaseStore.RateWindow(2, 60);
        assertThat(service.checkRateLimit("user-1").remaining()).isEqualTo(8);
        assertThat(store.lastKey).isEqualTo("rate-limit:user-1");
        assertThat(store.lastWindowSeconds).isEqualTo(60);

        store.rateResult = new RedisUseCaseStore.RateWindow(11, 17);
        assertThatThrownBy(() -> service.checkRateLimit("user-1"))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(17L);
    }

    @Test
    void lockUsesLeaseAndRequiresTheSameTokenToRelease() {
        FakeStore store = new FakeStore();
        RedisUseCaseService service = service(store);
        RedisUseCaseService.LockResult lock = service.acquireLock("daily-report");

        assertThat(lock.token()).isNotBlank();
        assertThat(lock.leaseMillis()).isEqualTo(10_000);
        assertThat(store.lastKey).isEqualTo("lock:daily-report");
        assertThat(store.lastLeaseMillis).isEqualTo(10_000);

        service.releaseLock("daily-report", lock.token());
        assertThat(store.lastToken).isEqualTo(lock.token());

        store.releaseAllowed = false;
        assertThatThrownBy(() -> service.releaseLock("daily-report", "another-token"))
                .isInstanceOf(LockConflictException.class);
    }

    private static RedisUseCaseService service(FakeStore store) {
        return new RedisUseCaseService(store, 10, 60, 10_000);
    }

    private static class FakeStore implements RedisUseCaseStore {
        private String lastKey;
        private String lastToken;
        private int lastWindowSeconds;
        private long lastLeaseMillis;
        private RateWindow rateResult = new RateWindow(1, 60);
        private boolean releaseAllowed = true;
        private long count;

        @Override
        public long incrementCounter(String key) {
            lastKey = key;
            return ++count;
        }

        @Override
        public Long getCounter(String key) {
            lastKey = key;
            return count == 0 ? null : count;
        }

        @Override
        public RateWindow incrementRateWindow(String key, int windowSeconds) {
            lastKey = key;
            lastWindowSeconds = windowSeconds;
            return rateResult;
        }

        @Override
        public boolean acquireLock(String key, String token, long leaseMillis) {
            lastKey = key;
            lastToken = token;
            lastLeaseMillis = leaseMillis;
            return true;
        }

        @Override
        public boolean releaseLock(String key, String token) {
            lastKey = key;
            lastToken = token;
            return releaseAllowed;
        }
    }
}
