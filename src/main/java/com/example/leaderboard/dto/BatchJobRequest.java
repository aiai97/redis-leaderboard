package com.example.leaderboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchJobRequest(
        @NotEmpty @Size(max = 1000) List<@Min(0) @Max(1_000_000_000L) Long> values) {}
