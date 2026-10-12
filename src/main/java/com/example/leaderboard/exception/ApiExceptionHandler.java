package com.example.leaderboard.exception;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(PlayerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> playerNotFound(PlayerNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<Map<String, Object>> rateLimitExceeded(RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(Map.of("error", exception.getMessage(), "retryAfterSeconds", exception.retryAfterSeconds()));
    }

    @ExceptionHandler(LockConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> lockConflict(LockConflictException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler({
            BatchJobNotFoundException.class,
            ProductNotFoundException.class,
            SessionNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> resourceNotFound(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }
}
