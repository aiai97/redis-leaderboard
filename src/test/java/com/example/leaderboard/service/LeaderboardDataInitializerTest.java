package com.example.leaderboard.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

class LeaderboardDataInitializerTest {
    private static final String KEY = "leaderboard:test";

    @Test
    void initializesSamplePlayersWhenLeaderboardIsEmpty() {
        LeaderboardStore store = mock(LeaderboardStore.class);
        when(store.count(KEY)).thenReturn(0L);

        new LeaderboardDataInitializer(store, KEY).run(mock(ApplicationArguments.class));

        verify(store).setScore(KEY, "alice", 1200.0);
        verify(store).setScore(KEY, "bob", 980.0);
        verify(store).setScore(KEY, "charlie", 860.0);
    }

    @Test
    void leavesExistingLeaderboardDataUnchanged() {
        LeaderboardStore store = mock(LeaderboardStore.class);
        when(store.count(KEY)).thenReturn(1L);

        new LeaderboardDataInitializer(store, KEY).run(mock(ApplicationArguments.class));

        verify(store).count(KEY);
        verifyNoMoreInteractions(store);
    }
}
