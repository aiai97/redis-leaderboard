package com.example.leaderboard.dto;

public record LockResponse(String name, String token, long leaseMillis) {}
