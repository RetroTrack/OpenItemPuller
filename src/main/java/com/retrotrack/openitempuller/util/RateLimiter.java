package com.retrotrack.openitempuller.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player token bucket measured in game ticks. A bucket starts full ({@code capacity} requests may
 * arrive back to back) and regains one token every {@code ticksPerToken} ticks.
 */
public final class RateLimiter {
    /** The chest list scans chunks, so it gets a small burst and a slow refill. */
    public static final RateLimiter CHECK = new RateLimiter(2, 10);
    /**
     * One payload per click. The small burst only covers the rare click whose payload had to be split because
     * it carries variants with very large components (see {@code PullItemsPayload.split}).
     */
    public static final RateLimiter PULL = new RateLimiter(4, 10);

    private record Bucket(double tokens, long lastTick) {}

    private final int capacity;
    private final int ticksPerToken;
    private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    /** Package-private so tests can make their own limiter; the shared ones are {@link #CHECK} and {@link #PULL}. */
    RateLimiter(int capacity, int ticksPerToken) {
        this.capacity = capacity;
        this.ticksPerToken = ticksPerToken;
    }

    /** @return true if the request may proceed; false if the player is over the limit. */
    public boolean tryAcquire(ServerPlayer player) {
        return tryAcquire(player.getUUID(), player.level().getGameTime());
    }

    /** Same as {@link #tryAcquire(ServerPlayer)}, with the player's id and the current game tick given directly. */
    boolean tryAcquire(UUID id, long now) {
        double tokens = capacity;
        Bucket previous = buckets.get(id);
        if (previous != null) {
            long elapsed = Math.max(0, now - previous.lastTick());
            tokens = Math.min(capacity, previous.tokens() + (double) elapsed / ticksPerToken);
        }

        boolean allowed = tokens >= 1;
        buckets.put(id, new Bucket(allowed ? tokens - 1 : tokens, now));
        purgeIdle(now);
        return allowed;
    }

    /** Drops buckets that have been idle long enough to be full again, so offline players don't pile up. */
    private void purgeIdle(long now) {
        if (buckets.size() <= 256) return;
        long idleTicks = (long) capacity * ticksPerToken;
        buckets.values().removeIf(b -> now - b.lastTick() > idleTicks);
    }
}
