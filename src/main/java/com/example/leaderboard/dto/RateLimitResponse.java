package com.example.leaderboard.dto;

public record RateLimitResponse(boolean allowed, long remaining, long retryAfterSeconds) {}
