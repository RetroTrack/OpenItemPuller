package com.retrotrack.openitempuller.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lets other mods (claim / protection mods, permission mods) veto access to a container.
 * Register a listener and return {@code false} to deny the player access to {@code pos}.
 * Every listener must return {@code true} for access to be granted.
 */
@FunctionalInterface
public interface PullPermissionCallback {
    Event<PullPermissionCallback> EVENT = EventFactory.createArrayBacked(PullPermissionCallback.class,
            listeners -> (player, level, pos) -> {
                for (PullPermissionCallback listener : listeners) {
                    if (!listener.canAccess(player, level, pos)) return false;
                }
                return true;
            });

    boolean canAccess(ServerPlayer player, ServerLevel level, BlockPos pos);
}
