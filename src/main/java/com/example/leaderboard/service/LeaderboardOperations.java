package com.example.leaderboard.service;

import com.example.leaderboard.dto.LeaderboardEntry;
import com.example.leaderboard.dto.LeaderboardPage;

public interface LeaderboardOperations {
    LeaderboardEntry setScore(String playerId, double score);
    LeaderboardEntry incrementScore(String playerId, double delta);
    LeaderboardEntry getPlayer(String playerId);
    LeaderboardPage getPage(int page, int size);
    boolean removePlayer(String playerId);
}
