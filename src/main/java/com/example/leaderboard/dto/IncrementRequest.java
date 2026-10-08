package com.example.leaderboard.dto;

import jakarta.validation.constraints.NotNull;

public record IncrementRequest(@NotNull Double delta) {
}
