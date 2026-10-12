package com.example.leaderboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.resps.StreamEntry;

class BatchJobServiceTest {
    @Test
    void submissionPersistsJobAndProcessingAcknowledgesEntries() {
        FakeStore store = new FakeStore();
        BatchJobService service = new BatchJobService(store);

        BatchJobService.JobStatus submitted = service.submit(List.of(2L, 3L, 5L));
        assertThat(submitted.total()).isEqualTo(3);
        assertThat(submitted.status()).isEqualTo("queued");

        store.newEntries = List.of(entry("1-0", submitted.batchId(), "2"), entry("2-0", submitted.batchId(), "3"));
        assertThat(service.processNext("worker-test")).isEqualTo(2);
        assertThat(store.acknowledged).containsExactly("1-0", "2-0");

        store.pendingEntries = List.of(entry("3-0", submitted.batchId(), "5"));
        assertThat(service.processNext("worker-test")).isEqualTo(1);
        assertThat(store.newEntries).hasSize(2);
        assertThat(store.acknowledged).containsExactly("1-0", "2-0", "3-0");
    }

    private static StreamEntry entry(String id, String batchId, String value) {
        return new StreamEntry(new StreamEntryID(id), Map.of("batchId", batchId, "value", value));
    }

    private static class FakeStore implements BatchJobStore {
        private final List<String> acknowledged = new ArrayList<>();
        private List<StreamEntry> pendingEntries = List.of();
        private List<StreamEntry> newEntries = List.of();
        private BatchJobService.JobStatus status;

        @Override
        public void createBatch(String batchId, List<Long> values) {
            status = new BatchJobService.JobStatus(batchId, values.size(), 0, 0, "queued");
        }

        @Override public List<StreamEntry> readPending(String consumer) { return pendingEntries; }
        @Override public List<StreamEntry> readNew(String consumer) { return newEntries; }

        @Override
        public void applyAndAcknowledge(StreamEntry entry, String consumer) {
            acknowledged.add(entry.getID().toString());
        }

        @Override public BatchJobService.JobStatus getBatch(String batchId) { return status; }
    }
}
