package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.api.PullPermissionCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.LockCode;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import static com.retrotrack.openitempuller.ItemPuller.serverConfig;

/**
 * Server-side checks for every position that arrives from a client (or is about to be sent to one).
 * The client is never trusted: radius, loaded chunk, container type, loot table, lock and
 * third-party permissions are all re-validated here.
 */
public final class PullValidator {
    /** Extra blocks of slack so a chest at the edge of the radius survives the player taking a step. */
    private static final double RANGE_SLACK = 2.0;

    private PullValidator() {}

    public static boolean isInRange(ServerPlayer player, BlockPos pos, int radius) {
        double max = radius + RANGE_SLACK;
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= max * max;
    }

    /**
     * Same rule as vanilla's {@code canOpen} (spectators, or the lock's item in the main hand), but silent:
     * {@code canOpen} sends a message and plays a sound on failure, which would repeat for every locked
     * container each time the list is built.
     */
    private static boolean isLockedFor(ServerPlayer player, BlockEntity blockEntity) {
        if (!(blockEntity instanceof BaseContainerBlockEntity)) return false;
        if (player.isSpectator()) return false;
        LockCode lock = blockEntity.collectComponents().get(DataComponents.LOCK);
        return lock != null && lock != LockCode.NO_LOCK && !lock.unlocksWith(player.getMainHandItem());
    }

    /**
     * @return true if {@code player} may take items from / insert items into the container at {@code pos}.
     * <p>
     * A double chest is one inventory: listing and transfer both go through {@code getContainerAt}, which
     * merges the two halves. So every half has to pass the checks, not only the one {@code pos} points at;
     * otherwise a free half would be a way into a locked, out-of-range or claimed neighbour.
     */
    public static boolean canAccess(ServerPlayer player, BlockPos pos, int radius) {
        // player.level() is the player's own dimension, so a position in another dimension can never match.
        if (!(player.level() instanceof ServerLevel level)) return false;
        if (!canAccessSingle(player, level, pos, radius)) return false;

        // canAccessSingle has established that the chunk at pos is loaded
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos partner = BlockEntityUtil.getConnectedChestPos(player, pos);
            // A half whose other half cannot be found (not loaded, mid-break) is denied rather than guessed at.
            if (partner == null) return false;
            return canAccessSingle(player, level, partner, radius);
        }
        return true;
    }

    /** All checks for exactly one block position; {@link #canAccess} adds the other half of a double chest. */
    private static boolean canAccessSingle(ServerPlayer player, ServerLevel level, BlockPos pos, int radius) {
        if (!level.isInWorldBounds(pos)) return false;
        if (!level.hasChunkAt(pos)) return false;                 // must already be loaded, never force-load
        if (!isInRange(player, pos, radius)) return false;
        if (!BlockEntitySearchUtil.isValidBlockEntity(pos, level)) return false;
        // Server-side blacklist (server.json "blocked_blocks"): ender chests by default
        String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        if (serverConfig().isBlocked(blockId)) return false;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RandomizableContainerBlockEntity lootable && lootable.getLootTable() != null) {
            return false;                                          // unopened loot chests
        }
        if (isLockedFor(player, blockEntity)) return false;
        return PullPermissionCallback.EVENT.invoker().canAccess(player, level, pos);
    }
}
