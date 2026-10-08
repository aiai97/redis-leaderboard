package com.example.leaderboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.leaderboard.dto.LeaderboardEntry;
import com.example.leaderboard.dto.LeaderboardPage;
import com.example.leaderboard.exception.PlayerNotFoundException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.resps.Tuple;

class LeaderboardServiceTest {
    private static final String KEY = "leaderboard:test";

    @Test
    void setScoreReturnsOneBasedRank() {
        InMemoryStore store = new InMemoryStore();
        LeaderboardEntry entry = new LeaderboardService(store, KEY).setScore("alice", 1200.0);
        assertThat(entry).isEqualTo(new LeaderboardEntry("alice", 1200.0, 1));
        assertThat(store.lastSet).isEqualTo("alice");
    }

    @Test
    void incrementScoreUsesAtomicStoreOperation() {
        InMemoryStore store = new InMemoryStore();
        store.scores.put("alice", 1200.0);
        LeaderboardEntry entry = new LeaderboardService(store, KEY).incrementScore("alice", 50.0);
        assertThat(entry).isEqualTo(new LeaderboardEntry("alice", 1250.0, 1));
        assertThat(store.lastIncrement).isEqualTo("alice");
    }

    @Test
    void pageKeepsAbsoluteRanks() {
        InMemoryStore store = new InMemoryStore();
        store.scores.putAll(Map.of("alice", 100.0, "bob", 90.0, "carol", 80.0, "dave", 70.0));
        LeaderboardPage page = new LeaderboardService(store, KEY).getPage(1, 2);
        assertThat(page.entries()).containsExactly(new LeaderboardEntry("carol", 80.0, 3),
                new LeaderboardEntry("dave", 70.0, 4));
        assertThat(page.total()).isEqualTo(4);
    }

    @Test
    void unknownPlayerReturnsNotFoundDomainError() {
        assertThatThrownBy(() -> new LeaderboardService(new InMemoryStore(), KEY).getPlayer("missing"))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    private static class InMemoryStore implements LeaderboardStore {
        private final Map<String, Double> scores = new HashMap<>();
        private String lastSet;
        private String lastIncrement;

        @Override public void setScore(String key, String playerId, double score) { scores.put(playerId, score); lastSet = playerId; }
        @Override public void incrementScore(String key, String playerId, double delta) { scores.merge(playerId, delta, Double::sum); lastIncrement = playerId; }
        @Override public Double getScore(String key, String playerId) { return scores.get(playerId); }
        @Override public Long getReverseRank(String key, String playerId) {
            int index = sortedPlayers().indexOf(playerId);
            return index < 0 ? null : (long) index;
        }
        @Override public List<Tuple> getReverseRangeWithScores(String key, long start, long end) {
            return sortedPlayers().stream().skip(start).limit(end - start + 1)
                    .map(player -> new Tuple(player, scores.get(player))).toList();
        }
        @Override public long count(String key) { return scores.size(); }
        @Override public boolean remove(String key, String playerId) { return scores.remove(playerId) != null; }

        private List<String> sortedPlayers() {
            return scores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                    .map(Map.Entry::getKey).toList();
        }
    }
}
