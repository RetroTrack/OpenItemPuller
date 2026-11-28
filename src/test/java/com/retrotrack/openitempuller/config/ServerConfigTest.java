package com.retrotrack.openitempuller.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {
    private static final Gson GSON = new Gson();

    private static ServerConfig parse(String json) {
        return GSON.fromJson(json, ServerConfig.class);
    }

    @Test
    void missingFieldsFallBackToDefaults() {
        ServerConfig config = parse("{}");
        assertEquals(ServerConfig.DEFAULT_MAX_RADIUS, config.maxRadius());
        assertEquals(ServerConfig.DEFAULT_BLOCKED_BLOCKS, config.blockedBlocks());
    }

    @Test
    void radiusBelowOneFallsBackToDefault() {
        assertEquals(ServerConfig.DEFAULT_MAX_RADIUS, parse("{\"max_radius\": 0}").maxRadius());
        assertEquals(ServerConfig.DEFAULT_MAX_RADIUS, parse("{\"max_radius\": -5}").maxRadius());
    }

    @Test
    void radiusIsClampedToTheHardLimit() {
        assertEquals(ServerConfig.MAX_RADIUS_LIMIT, parse("{\"max_radius\": 100000}").maxRadius());
        assertEquals(ServerConfig.MAX_RADIUS_LIMIT, new ServerConfig(129).maxRadius());
        assertEquals(64, new ServerConfig(64).maxRadius());
    }

    @Test
    void enderChestsAreBlockedByDefault() {
        assertTrue(ServerConfig.DEFAULT.isBlocked("minecraft:ender_chest"));
        assertFalse(ServerConfig.DEFAULT.isBlocked("minecraft:chest"));
    }

    @Test
    void anEmptyBlockListBlocksNothing() {
        ServerConfig config = parse("{\"blocked_blocks\": []}");
        assertFalse(config.isBlocked("minecraft:ender_chest"));
    }

    @Test
    void blockedIdsAreTrimmedAndLowercased() {
        ServerConfig config = parse("{\"blocked_blocks\": [\"  Minecraft:Barrel \"]}");
        assertTrue(config.isBlocked("minecraft:barrel"));
    }

    @Test
    void nullEntriesInTheBlockListAreIgnored() {
        ServerConfig config = new ServerConfig(16, java.util.Arrays.asList("minecraft:barrel", null));
        assertEquals(List.of("minecraft:barrel"), config.blockedBlocks());
    }
}
