package com.example.leaderboard.service;

import com.example.leaderboard.dto.BatchJobResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import redis.clients.jedis.resps.StreamEntry;

@Service
public class BatchJobService {
    private final BatchJobStore store;

    public BatchJobService(BatchJobStore store) {
        this.store = store;
    }

    public JobStatus submit(List<Long> values) {
        String batchId = UUID.randomUUID().toString();
        store.createBatch(batchId, values);
        return store.getBatch(batchId);
    }

    public JobStatus get(String batchId) {
        return store.getBatch(batchId);
    }

    public int processNext(String consumer) {
        List<StreamEntry> entries = store.readPending(consumer);
        if (entries.isEmpty()) {
            entries = store.readNew(consumer);
        }
        for (StreamEntry entry : entries) {
            store.applyAndAcknowledge(entry, consumer);
        }
        return entries.size();
    }

    public record JobStatus(String batchId, long total, long processed, long sum, String status) {
        public BatchJobResponse response() {
            return new BatchJobResponse(batchId, total, processed, sum, status);
        }
    }
}
