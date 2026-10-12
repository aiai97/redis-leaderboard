package com.example.leaderboard.exception;

public class BatchJobNotFoundException extends RuntimeException {
    public BatchJobNotFoundException(String batchId) {
        super("Batch job not found: " + batchId);
    }
}
