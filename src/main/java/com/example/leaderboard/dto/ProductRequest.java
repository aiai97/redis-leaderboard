package com.example.leaderboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(@NotBlank String name, @PositiveOrZero long priceInCents) {}
