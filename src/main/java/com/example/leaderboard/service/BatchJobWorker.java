package com.example.leaderboard.service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatchJobWorker {
    private static final Logger log = LoggerFactory.getLogger(BatchJobWorker.class);
    private final JedisBatchJobStore store;
    private final BatchJobService service;
    private final String consumer = "worker-" + UUID.randomUUID();

    public BatchJobWorker(JedisBatchJobStore store, BatchJobService service) {
        this.store = store;
        this.service = service;
    }

    @PostConstruct
    void initialize() {
        store.initializeConsumerGroup();
    }

    @Scheduled(fixedDelayString = "${redis-use-cases.batch.poll-delay-millis}")
    public void processBatch() {
        int processed = service.processNext(consumer);
        if (processed > 0) {
            log.info("batch.messages_processed count={} consumer={}", processed, consumer);
        }
    }
}
