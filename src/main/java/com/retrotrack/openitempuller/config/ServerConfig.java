package com.retrotrack.openitempuller.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Server-side settings (config/openitempuller/server.json). Only the server reads this file;
 * clients learn the limit through the payloads. In singleplayer the integrated server uses it too.
 *
 * @param maxRadius     the largest pull radius the server accepts, in blocks. Values above
 *                      {@link #MAX_RADIUS_LIMIT} are clamped down so a misconfigured file can't make the
 *                      server scan millions of chunk positions per request.
 * @param blockedBlocks block ids (for example {@code "minecraft:ender_chest"}) that are never listed and never
 *                      pulled from or into, whatever else allows it. A missing entry means
 *                      {@link #DEFAULT_BLOCKED_BLOCKS}; an empty list blocks nothing.
 */
public record ServerConfig(
        @SerializedName("max_radius") int maxRadius,
        @SerializedName("blocked_blocks") List<String> blockedBlocks) {
    public static final int DEFAULT_MAX_RADIUS = 16;
    /** Hard upper bound (8 chunks each way = 17x17 chunks scanned at most). Not configurable. */
    public static final int MAX_RADIUS_LIMIT = 128;
    public static final List<String> DEFAULT_BLOCKED_BLOCKS = List.of("minecraft:ender_chest");
    public static final ServerConfig DEFAULT = new ServerConfig(DEFAULT_MAX_RADIUS);

    public ServerConfig {
        // Gson passes 0 for a missing field, so anything below 1 falls back to the default.
        if (maxRadius < 1) maxRadius = DEFAULT_MAX_RADIUS;
        maxRadius = Math.min(maxRadius, MAX_RADIUS_LIMIT);
        blockedBlocks = blockedBlocks == null
                ? DEFAULT_BLOCKED_BLOCKS
                : blockedBlocks.stream().filter(Objects::nonNull)
                        .map(id -> id.trim().toLowerCase(Locale.ROOT)).toList();
    }

    public ServerConfig(int maxRadius) {
        this(maxRadius, null);
    }

    /** @param blockId a block id such as {@code "minecraft:chest"} */
    public boolean isBlocked(String blockId) {
        return blockedBlocks.contains(blockId);
    }
}
