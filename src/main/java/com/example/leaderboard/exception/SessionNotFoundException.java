package com.example.leaderboard.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException() {
        super("Session not found or expired");
    }
}
