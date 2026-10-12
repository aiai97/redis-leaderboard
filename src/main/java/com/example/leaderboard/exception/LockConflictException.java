package com.example.leaderboard.exception;

public class LockConflictException extends RuntimeException {
    public LockConflictException(String message) {
        super(message);
    }
}
