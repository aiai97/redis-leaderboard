package com.example.leaderboard.dto;

public record BatchJobResponse(String batchId, long total, long processed, long sum, String status) {}
