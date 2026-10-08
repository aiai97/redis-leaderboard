package com.example.leaderboard.dto;

import java.util.List;

public record LeaderboardPage(List<LeaderboardEntry> entries, int page, int size, long total) {
}
