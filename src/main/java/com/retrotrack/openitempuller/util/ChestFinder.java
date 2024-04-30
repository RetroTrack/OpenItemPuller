package com.retrotrack.openitempuller.util;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChestFinder {

    public static List<LootableContainerBlockEntity> findSortedLootableContainerBlockEntitiesAroundPlayer(ServerWorld world, ServerPlayerEntity player, int radius) {
        return sortByDistance(findLootableContainerBlockEntitiesAroundPlayer(world, player, radius), player);
    }

    public static LootableContainerBlockEntity checkLootableContainerBlockEntity(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof LootableContainerBlockEntity)) return null;
        if (((LootableContainerBlockEntity) blockEntity).getLootTableId() != null) return null;
        return ((LootableContainerBlockEntity) blockEntity);
    }


    public static List<LootableContainerBlockEntity> findLootableContainerBlockEntitiesAroundPlayer(ServerWorld world, ServerPlayerEntity player, int radius) {
        List<LootableContainerBlockEntity> list = new ArrayList<>();

        // Calculate the chunk range based on the radius
        int chunkRadius = (int) Math.ceil((double) radius / 16);
        ChunkPos playerChunkPos = new ChunkPos(player.getBlockPos());

        // Iterate over chunks within the chunk radius
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkPos.x + dx, playerChunkPos.z + dz);
                Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

                // Iterate over block entities in the chunk
                for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
                    BlockEntity blockEntity = world.getBlockEntity(blockPos);
                    if (blockEntity instanceof LootableContainerBlockEntity) {
                        BlockPos chestPos = blockEntity.getPos();
                        // Check if the chest is within the radius
                        if (player.getBlockPos().getSquaredDistance(chestPos) <= radius * radius && ((LootableContainerBlockEntity)blockEntity).getLootTableId() == null) {
                            BlockPos connectedChest = ChestConnectionChecker.getConnectedChestPos(player, chestPos);
                            if(connectedChest == null) list.add((LootableContainerBlockEntity) blockEntity);
                            else if(!list.contains((LootableContainerBlockEntity) world.getBlockEntity(connectedChest)))list.add((LootableContainerBlockEntity) blockEntity);
                        }
                    }
                }
            }
        }
        return list;
    }

    public static List<LootableContainerBlockEntity> sortByDistance(List<LootableContainerBlockEntity> blockPosList, ServerPlayerEntity player) {
        // Sort the blockPosList based on distance to the player
        blockPosList.sort(Comparator.comparingDouble(pos -> pos.getPos().getSquaredDistance(player.getX(), player.getY(), player.getZ())));

        return blockPosList;
    }
}
