package com.example.leaderboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateSessionRequest(@NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String userId) {}
