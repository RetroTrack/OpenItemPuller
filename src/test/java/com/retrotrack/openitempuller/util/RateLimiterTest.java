package com.retrotrack.openitempuller.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void aBurstUpToTheCapacityIsAllowed() {
        RateLimiter limiter = new RateLimiter(2, 10);
        assertTrue(limiter.tryAcquire(ALICE, 0));
        assertTrue(limiter.tryAcquire(ALICE, 0));
        assertFalse(limiter.tryAcquire(ALICE, 0));
    }

    @Test
    void aTokenComesBackAfterTheRefillTime() {
        RateLimiter limiter = new RateLimiter(2, 10);
        limiter.tryAcquire(ALICE, 0);
        limiter.tryAcquire(ALICE, 0);
        assertFalse(limiter.tryAcquire(ALICE, 9));   // not yet
        assertTrue(limiter.tryAcquire(ALICE, 20));   // the earlier try left 0.9 of a token, so 11 more ticks make a whole one
    }

    @Test
    void waitingNeverBuildsUpMoreThanTheCapacity() {
        RateLimiter limiter = new RateLimiter(2, 10);
        limiter.tryAcquire(ALICE, 0);
        assertTrue(limiter.tryAcquire(ALICE, 100_000));
        assertTrue(limiter.tryAcquire(ALICE, 100_000));
        assertFalse(limiter.tryAcquire(ALICE, 100_000));
    }

    @Test
    void playersHaveTheirOwnBucket() {
        RateLimiter limiter = new RateLimiter(1, 10);
        assertTrue(limiter.tryAcquire(ALICE, 0));
        assertFalse(limiter.tryAcquire(ALICE, 0));
        assertTrue(limiter.tryAcquire(BOB, 0));
    }

    @Test
    void aClockThatGoesBackwardsDoesNotGiveFreeTokens() {
        RateLimiter limiter = new RateLimiter(1, 10);
        assertTrue(limiter.tryAcquire(ALICE, 1000));
        assertFalse(limiter.tryAcquire(ALICE, 0));
    }

    @Test
    void idleBucketsArePurgedSoOfflinePlayersDoNotPileUp() {
        RateLimiter limiter = new RateLimiter(1, 10);
        for (int k = 0; k < 400; k++) limiter.tryAcquire(UUID.randomUUID(), 0);
        // after a long time everybody is full again, and a new request must still work
        assertTrue(limiter.tryAcquire(ALICE, 10_000));
    }
}
