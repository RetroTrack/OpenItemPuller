package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockEntitySearchUtil {

    public static List<BlockEntity> findSortedBlockEntitiesAroundPlayer(ServerLevel world, ServerPlayer player, int radius) {
        return sortByDistance(findBlockEntitiesAroundPlayer(world, player, radius), player);
    }

    public static BlockEntity isUnlocked(ServerPlayer player, BlockPos pos) {
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof RandomizableContainerBlockEntity lootable && lootable.getLootTable() != null) return null;
        return blockEntity;
    }

    public static List<BlockEntity> findBlockEntitiesAroundPlayer(ServerLevel world, ServerPlayer player, int radius) {
        List<BlockEntity> list = new ArrayList<>();

        int chunkRadius = (int) Math.ceil((double) radius / 16);
        ChunkPos playerChunkPos = new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkPos.x() + dx, playerChunkPos.z() + dz);
                ChunkAccess chunk = world.getChunk(chunkPos.x(), chunkPos.z());

                for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
                    BlockEntity blockEntity = world.getBlockEntity(blockPos);
                    if (blockEntity instanceof RandomizableContainerBlockEntity lootable) {
                        if (player.blockPosition().distSqr(blockPos) <= radius * radius && lootable.getLootTable() == null) {
                            addUnlessDoubleChestHalf(list, world, player, blockPos, blockEntity);
                        }
                    } else if (blockEntity instanceof Container) {
                        if (player.blockPosition().distSqr(blockPos) <= radius * radius) {
                            addUnlessDoubleChestHalf(list, world, player, blockPos, blockEntity);
                        }
                    } else if (isValidBlockEntity(blockPos, world)) {
                        list.add(blockEntity);
                    }
                }
            }
        }
        return list;
    }

    private static void addUnlessDoubleChestHalf(List<BlockEntity> list, ServerLevel world, ServerPlayer player,
                                                 BlockPos blockPos, BlockEntity blockEntity) {
        BlockPos connectedChest = BlockEntityUtil.getConnectedChestPos(player, blockPos);
        if (connectedChest == null) list.add(blockEntity);
        else if (!list.contains(world.getBlockEntity(connectedChest))) list.add(blockEntity);
    }

    public static boolean isValidBlockEntity(BlockPos blockPos, Level world) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);
        return blockEntity instanceof Container || ItemStorage.SIDED.find(world, blockPos, null) != null;
    }

    public static List<BlockEntity> sortByDistance(List<BlockEntity> blockPosList, ServerPlayer player) {
        blockPosList.sort(Comparator.comparingDouble(be ->
                be.getBlockPos().distToLowCornerSqr(player.getX(), player.getY(), player.getZ())));
        return blockPosList;
    }

    public static ArrayList<DecodedBlockEntity> sortByItemCount(ArrayList<DecodedBlockEntity> decodedBlockEntities, Item item) {
        decodedBlockEntities.sort(Comparator.comparingInt(chest -> -chest.items().getOrDefault(item, 0)));
        return decodedBlockEntities;
    }
}
