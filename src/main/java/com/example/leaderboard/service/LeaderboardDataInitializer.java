package com.example.leaderboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardDataInitializer.class);
    private final LeaderboardStore store;
    private final String leaderboardKey;

    public LeaderboardDataInitializer(
            LeaderboardStore store,
            @Value("${leaderboard.key}") String leaderboardKey) {
        this.store = store;
        this.leaderboardKey = leaderboardKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (store.count(leaderboardKey) > 0) {
            return;
        }

        store.setScore(leaderboardKey, "alice", 1200.0);
        store.setScore(leaderboardKey, "bob", 980.0);
        store.setScore(leaderboardKey, "charlie", 860.0);
        log.info("leaderboard.sample_data_initialized key={} players=3", leaderboardKey);
    }
}
