package com.example.leaderboard.dto;

import jakarta.validation.constraints.NotBlank;

public record LockRequest(@NotBlank String token) {}
