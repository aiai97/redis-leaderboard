package com.example.leaderboard.controller;

import com.example.leaderboard.dto.LockRequest;
import com.example.leaderboard.dto.LockResponse;
import com.example.leaderboard.dto.NamedCounter;
import com.example.leaderboard.dto.RateLimitResponse;
import com.example.leaderboard.service.RedisUseCaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class RedisUseCaseController {
    private static final String SAFE_NAME = "[a-zA-Z0-9:_-]{1,80}";

    private final RedisUseCaseService service;

    public RedisUseCaseController(RedisUseCaseService service) {
        this.service = service;
    }

    @PostMapping("/counters/{name}/increment")
    public NamedCounter incrementCounter(@PathVariable @Pattern(regexp = SAFE_NAME) String name) {
        return new NamedCounter(name, service.incrementCounter(name));
    }

    @GetMapping("/counters/{name}")
    public NamedCounter getCounter(@PathVariable @Pattern(regexp = SAFE_NAME) String name) {
        return new NamedCounter(name, service.getCounter(name));
    }

    @PostMapping("/demo/rate-limit/{clientId}")
    public RateLimitResponse checkRateLimit(@PathVariable @Pattern(regexp = SAFE_NAME) String clientId) {
        RedisUseCaseService.RateLimitResult result = service.checkRateLimit(clientId);
        return new RateLimitResponse(true, result.remaining(), result.retryAfterSeconds());
    }

    @PostMapping("/locks/{name}")
    @ResponseStatus(HttpStatus.CREATED)
    public LockResponse acquireLock(@PathVariable @Pattern(regexp = SAFE_NAME) String name) {
        RedisUseCaseService.LockResult result = service.acquireLock(name);
        return new LockResponse(name, result.token(), result.leaseMillis());
    }

    @DeleteMapping("/locks/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseLock(
            @PathVariable @Pattern(regexp = SAFE_NAME) String name,
            @Valid @RequestBody LockRequest request) {
        service.releaseLock(name, request.token());
    }
}
