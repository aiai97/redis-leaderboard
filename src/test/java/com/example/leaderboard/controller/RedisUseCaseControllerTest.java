package com.example.leaderboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.leaderboard.exception.ApiExceptionHandler;
import com.example.leaderboard.service.RedisUseCaseService;
import com.example.leaderboard.service.RedisUseCaseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RedisUseCaseControllerTest {
    private final FakeStore store = new FakeStore();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RedisUseCaseController(new RedisUseCaseService(store, 10, 60, 10_000)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void counterIncrementReturnsCurrentValue() throws Exception {
        mockMvc.perform(post("/api/counters/article:42:views/increment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("article:42:views"))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void exceededRateLimitReturnsRetryAfterHeader() throws Exception {
        store.rateWindow = new RedisUseCaseStore.RateWindow(11, 23);

        mockMvc.perform(post("/api/demo/rate-limit/client-1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "23"))
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
    }

    @Test
    void lockCanBeReleasedWithItsOwnerToken() throws Exception {
        mockMvc.perform(post("/api/locks/daily-report"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(delete("/api/locks/daily-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"owner-token\"}"))
                .andExpect(status().isNoContent());
    }

    private static class FakeStore implements RedisUseCaseStore {
        private long counter;
        private RateWindow rateWindow = new RateWindow(1, 60);

        @Override public long incrementCounter(String key) { return ++counter; }
        @Override public Long getCounter(String key) { return counter == 0 ? null : counter; }
        @Override public RateWindow incrementRateWindow(String key, int windowSeconds) { return rateWindow; }
        @Override public boolean acquireLock(String key, String token, long leaseMillis) { return true; }
        @Override public boolean releaseLock(String key, String token) { return true; }
    }
}
