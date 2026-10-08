package com.example.leaderboard.dto;

import jakarta.validation.constraints.NotNull;

public record ScoreRequest(@NotNull Double score) {
}
