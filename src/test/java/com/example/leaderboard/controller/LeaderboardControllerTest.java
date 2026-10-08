package com.example.leaderboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.leaderboard.dto.LeaderboardEntry;
import com.example.leaderboard.dto.LeaderboardPage;
import com.example.leaderboard.exception.ApiExceptionHandler;
import com.example.leaderboard.service.LeaderboardOperations;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LeaderboardControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaderboardController(new FakeLeaderboard()))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void setScoreReturnsUpdatedEntry() throws Exception {
        mockMvc.perform(put("/api/leaderboard/players/alice/score")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"score\":1200}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.playerId").value("alice"))
                .andExpect(jsonPath("$.rank").value(1));
    }

    @Test
    void leaderboardPageIsSerialized() throws Exception {
        mockMvc.perform(get("/api/leaderboard?page=0&size=2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.entries[0].playerId").value("alice"));
    }

    private static class FakeLeaderboard implements LeaderboardOperations {
        @Override public LeaderboardEntry setScore(String playerId, double score) { return new LeaderboardEntry(playerId, score, 1); }
        @Override public LeaderboardEntry incrementScore(String playerId, double delta) { return new LeaderboardEntry(playerId, delta, 1); }
        @Override public LeaderboardEntry getPlayer(String playerId) { return new LeaderboardEntry(playerId, 1200.0, 1); }
        @Override public LeaderboardPage getPage(int page, int size) { return new LeaderboardPage(List.of(new LeaderboardEntry("alice", 1200.0, 1)), page, size, 1); }
        @Override public boolean removePlayer(String playerId) { return true; }
    }
}
