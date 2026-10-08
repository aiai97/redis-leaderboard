package com.example.leaderboard.service;

import com.example.leaderboard.dto.LeaderboardEntry;
import com.example.leaderboard.dto.LeaderboardPage;
import com.example.leaderboard.exception.PlayerNotFoundException;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.resps.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService implements LeaderboardOperations {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private final LeaderboardStore store;
    private final String leaderboardKey;

    public LeaderboardService(LeaderboardStore store, @Value("${leaderboard.key}") String leaderboardKey) {
        this.store = store;
        this.leaderboardKey = leaderboardKey;
    }

    /** Sets a player's absolute score. Redis sorted sets keep the order automatically. */
    public LeaderboardEntry setScore(String playerId, double score) {
        store.setScore(leaderboardKey, playerId, score);
        LeaderboardEntry entry = getPlayer(playerId);
        log.info("leaderboard.score_set playerId={} score={} rank={}", playerId, entry.score(), entry.rank());
        return entry;
    }

    /** Adds to a player's score and returns their updated position. */
    public LeaderboardEntry incrementScore(String playerId, double delta) {
        store.incrementScore(leaderboardKey, playerId, delta);
        LeaderboardEntry entry = getPlayer(playerId);
        log.info("leaderboard.score_incremented playerId={} delta={} score={} rank={}",
                playerId, delta, entry.score(), entry.rank());
        return entry;
    }

    public LeaderboardEntry getPlayer(String playerId) {
        Double score = store.getScore(leaderboardKey, playerId);
        Long zeroBasedRank = store.getReverseRank(leaderboardKey, playerId);
        if (score == null || zeroBasedRank == null) {
            log.warn("leaderboard.player_not_found playerId={}", playerId);
            throw new PlayerNotFoundException(playerId);
        }
        LeaderboardEntry entry = new LeaderboardEntry(playerId, score, zeroBasedRank + 1);
        log.debug("leaderboard.player_read playerId={} score={} rank={}", playerId, score, entry.rank());
        return entry;
    }

    public LeaderboardPage getPage(int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;
        List<Tuple> tuples = store.getReverseRangeWithScores(leaderboardKey, start, end);
        List<LeaderboardEntry> entries = new ArrayList<>(tuples.size());
        for (int index = 0; index < tuples.size(); index++) {
            Tuple tuple = tuples.get(index);
            entries.add(new LeaderboardEntry(tuple.getElement(), tuple.getScore(), start + index + 1));
        }
        long total = store.count(leaderboardKey);
        log.info("leaderboard.page_read page={} size={} returned={} total={}", page, size, entries.size(), total);
        return new LeaderboardPage(entries, page, size, total);
    }

    public boolean removePlayer(String playerId) {
        boolean removed = store.remove(leaderboardKey, playerId);
        log.info("leaderboard.player_removed playerId={} removed={}", playerId, removed);
        return removed;
    }
}
