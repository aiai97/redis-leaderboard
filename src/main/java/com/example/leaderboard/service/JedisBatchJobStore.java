package com.example.leaderboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAutoClaimParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.resps.StreamEntry;

@Repository
public class JedisBatchJobStore implements BatchJobStore {
    private static final String CREATE_BATCH_SCRIPT = """
            redis.call('HSET', KEYS[2],
                'expected', #ARGV / 2,
                'processed', 0,
                'sum', 0,
                'status', 'queued')
            for i = 1, (#ARGV / 2) do
                redis.call('XADD', KEYS[1], '*', 'batchId', ARGV[i], 'value', ARGV[i + (#ARGV / 2)])
            end
            return #ARGV / 2
            """;
    private static final String APPLY_AND_ACK_SCRIPT = """
            if redis.call('SADD', KEYS[2], ARGV[1]) == 1 then
                local processed = redis.call('HINCRBY', KEYS[1], 'processed', 1)
                redis.call('HINCRBY', KEYS[1], 'sum', ARGV[2])
                local expected = tonumber(redis.call('HGET', KEYS[1], 'expected'))
                if processed >= expected then
                    redis.call('HSET', KEYS[1], 'status', 'completed')
                    redis.call('EXPIRE', KEYS[2], 86400)
                    redis.call('EXPIRE', KEYS[1], 86400)
                else
                    redis.call('HSET', KEYS[1], 'status', 'running')
                end
            end
            return redis.call('XACK', KEYS[3], ARGV[3], ARGV[4])
            """;

    private final JedisPooled redis;
    private final String streamKey;
    private final String groupName;
    private final long claimIdleMillis;

    public JedisBatchJobStore(
            JedisPooled redis,
            @Value("${redis-use-cases.batch.stream-key}") String streamKey,
            @Value("${redis-use-cases.batch.group}") String groupName,
            @Value("${redis-use-cases.batch.claim-idle-millis}") long claimIdleMillis) {
        this.redis = redis;
        this.streamKey = streamKey;
        this.groupName = groupName;
        this.claimIdleMillis = claimIdleMillis;
    }

    public void initializeConsumerGroup() {
        try {
            redis.xgroupCreate(streamKey, groupName, new StreamEntryID(0, 0), true);
        } catch (JedisDataException exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("BUSYGROUP")) {
                throw exception;
            }
        }
    }

    @Override
    public void createBatch(String batchId, List<Long> values) {
        List<String> arguments = new ArrayList<>(values.size() * 2);
        for (Long value : values) {
            arguments.add(batchId);
        }
        for (Long value : values) {
            arguments.add(Long.toString(value));
        }
        redis.eval(CREATE_BATCH_SCRIPT, List.of(streamKey, jobKey(batchId)), arguments);
    }

    @Override
    public List<StreamEntry> readPending(String consumer) {
        Map.Entry<StreamEntryID, List<StreamEntry>> result = redis.xautoclaim(
                streamKey, groupName, consumer, claimIdleMillis, new StreamEntryID(0, 0),
                XAutoClaimParams.xAutoClaimParams().count(10));
        return result.getValue();
    }

    @Override
    public List<StreamEntry> readNew(String consumer) {
        List<Map.Entry<String, List<StreamEntry>>> result = redis.xreadGroup(
                groupName, consumer, XReadGroupParams.xReadGroupParams().count(10),
                Map.of(streamKey, StreamEntryID.UNRECEIVED_ENTRY));
        return result == null || result.isEmpty() ? List.of() : result.get(0).getValue();
    }

    @Override
    public void applyAndAcknowledge(StreamEntry entry, String consumer) {
        Map<String, String> fields = entry.getFields();
        String batchId = fields.get("batchId");
        String value = fields.get("value");
        if (batchId == null || value == null) {
            throw new IllegalStateException("Batch stream entry is missing required fields");
        }
        redis.eval(APPLY_AND_ACK_SCRIPT,
                List.of(jobKey(batchId), seenKey(batchId), streamKey),
                List.of(entry.getID().toString(), value, groupName, entry.getID().toString()));
    }

    @Override
    public BatchJobService.JobStatus getBatch(String batchId) {
        Map<String, String> values = redis.hgetAll(jobKey(batchId));
        if (values.isEmpty()) {
            return null;
        }
        return new BatchJobService.JobStatus(
                batchId,
                Long.parseLong(values.get("expected")),
                Long.parseLong(values.get("processed")),
                Long.parseLong(values.get("sum")),
                values.get("status"));
    }

    private String jobKey(String batchId) {
        return "batch-job:" + batchId;
    }

    private String seenKey(String batchId) {
        return "batch-job:" + batchId + ":processed";
    }
}
