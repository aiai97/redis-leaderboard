package com.example.leaderboard.service;

import java.util.List;
import redis.clients.jedis.resps.StreamEntry;

public interface BatchJobStore {
    void createBatch(String batchId, List<Long> values);
    List<StreamEntry> readPending(String consumer);
    List<StreamEntry> readNew(String consumer);
    void applyAndAcknowledge(StreamEntry entry, String consumer);
    BatchJobService.JobStatus getBatch(String batchId);
}
