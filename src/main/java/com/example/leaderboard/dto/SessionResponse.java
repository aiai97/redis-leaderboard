package com.example.leaderboard.dto;

public record SessionResponse(String token, String userId, long expiresInSeconds) {}
