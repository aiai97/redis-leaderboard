package com.example.leaderboard.controller;

import com.example.leaderboard.dto.IncrementRequest;
import com.example.leaderboard.dto.LeaderboardEntry;
import com.example.leaderboard.dto.LeaderboardPage;
import com.example.leaderboard.dto.ScoreRequest;
import com.example.leaderboard.service.LeaderboardOperations;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final LeaderboardOperations leaderboardService;

    public LeaderboardController(LeaderboardOperations leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PutMapping("/players/{playerId}/score")
    public LeaderboardEntry setScore(@PathVariable String playerId, @Valid @RequestBody ScoreRequest request) {
        return leaderboardService.setScore(playerId, request.score());
    }

    @PostMapping("/players/{playerId}/increment")
    public LeaderboardEntry incrementScore(@PathVariable String playerId, @Valid @RequestBody IncrementRequest request) {
        return leaderboardService.incrementScore(playerId, request.delta());
    }

    @GetMapping("/players/{playerId}")
    public LeaderboardEntry getPlayer(@PathVariable String playerId) {
        return leaderboardService.getPlayer(playerId);
    }

    @GetMapping
    public LeaderboardPage getLeaderboard(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return leaderboardService.getPage(page, size);
    }

    @DeleteMapping("/players/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlayer(@PathVariable String playerId) {
        leaderboardService.removePlayer(playerId);
    }
}
