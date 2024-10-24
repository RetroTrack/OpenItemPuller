package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.decoding.DecodedChest;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ChestFinder {

    public static List<LockableContainerBlockEntity> findSortedLootableContainerBlockEntitiesAroundPlayer(ServerWorld world, ServerPlayerEntity player, int radius) {
        return sortByDistance(findLootableContainerBlockEntitiesAroundPlayer(world, player, radius), player);
    }

    public static LockableContainerBlockEntity checkLockableContainerBlockEntity(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof LockableContainerBlockEntity)) return null;
        System.out.println(blockEntity.createNbtWithId().asString());
        if ((blockEntity instanceof LootableContainerBlockEntity) && (blockEntity.createNbtWithId().getString("LootTable") == null
                || Objects.equals(blockEntity.createNbtWithId().getString("LootTable"), ""))) return ((LockableContainerBlockEntity) blockEntity);
        return null;
    }


    public static List<LockableContainerBlockEntity> findLootableContainerBlockEntitiesAroundPlayer(ServerWorld world, ServerPlayerEntity player, int radius) {
        List<LockableContainerBlockEntity> list = new ArrayList<>();

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
                        if (player.getBlockPos().getSquaredDistance(chestPos) <= radius * radius && (blockEntity.createNbtWithId().getString("LootTable") == null
                                || Objects.equals(blockEntity.createNbtWithId().getString("LootTable"), ""))) {
                            BlockPos connectedChest = ChestConnectionChecker.getConnectedChestPos(player, chestPos);
                            if(connectedChest == null) list.add((LockableContainerBlockEntity) blockEntity);
                            else if(!list.contains((LockableContainerBlockEntity) world.getBlockEntity(connectedChest)))list.add((LockableContainerBlockEntity) blockEntity);
                        }
                    }else if (blockEntity instanceof LockableContainerBlockEntity) {
                        BlockPos chestPos = blockEntity.getPos();
                        // Check if the chest is within the radius
                        if (player.getBlockPos().getSquaredDistance(chestPos) <= radius * radius) {
                            BlockPos connectedChest = ChestConnectionChecker.getConnectedChestPos(player, chestPos);
                            if(connectedChest == null) list.add((LockableContainerBlockEntity) blockEntity);
                            else if(!list.contains((LockableContainerBlockEntity) world.getBlockEntity(connectedChest)))list.add((LockableContainerBlockEntity) blockEntity);
                        }
                    }
                }
            }
        }
        return list;
    }

    public static List<LockableContainerBlockEntity> sortByDistance(List<LockableContainerBlockEntity> blockPosList, ServerPlayerEntity player) {
        // Sort the blockPosList based on distance to the player
        blockPosList.sort(Comparator.comparingDouble(pos -> pos.getPos().getSquaredDistance(player.getX(), player.getY(), player.getZ())));

        return blockPosList;
    }
    public static ArrayList<DecodedChest> sortByItemCount(ArrayList<DecodedChest> decodedChests, Item item) {
        decodedChests.sort(Comparator.comparingInt(chest -> -chest.items().getOrDefault(item, 0)));
        return decodedChests;

    }
}
