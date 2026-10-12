package com.example.leaderboard.controller;

import com.example.leaderboard.dto.CreateSessionRequest;
import com.example.leaderboard.dto.SessionResponse;
import com.example.leaderboard.service.SessionService;
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
@RequestMapping("/api/sessions")
public class SessionController {
    private static final String TOKEN = "[a-f0-9-]{36}";
    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
        return service.create(request.userId());
    }

    @GetMapping("/{token}")
    public SessionResponse get(@PathVariable @Pattern(regexp = TOKEN) String token) {
        return service.get(token);
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Pattern(regexp = TOKEN) String token) {
        service.delete(token);
    }
}
