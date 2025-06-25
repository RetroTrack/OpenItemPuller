package com.retrotrack.openitempuller.util;
import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockEntitySearchUtil {

    public static List<BlockEntity> findSortedBlockEntitiesAroundPlayer(ServerWorld world, ServerPlayerEntity player, int radius) {
        return sortByDistance(findBlockEntitiesAroundPlayer(world, player, radius), player);
    }

    public static BlockEntity isUnlocked(ServerPlayerEntity player, BlockPos pos) {
        BlockEntity blockEntity = player.getServerWorld().getBlockEntity(pos);
        if (blockEntity instanceof LootableContainerBlockEntity && ((LootableContainerBlockEntity) blockEntity).getLootTable() != null) return null;

        return blockEntity;
    }


    public static List<BlockEntity> findBlockEntitiesAroundPlayer(ServerWorld world, ServerPlayerEntity player, int radius) {
        List<BlockEntity> list = new ArrayList<>();

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
                        // Check if the chest is within the radius
                        if (player.getBlockPos().getSquaredDistance(blockPos) <= radius * radius && ((LootableContainerBlockEntity)blockEntity).getLootTable() == null) {
                            BlockPos connectedChest = BlockEntityUtil.getConnectedChestPos(player, blockPos);
                            if(connectedChest == null) list.add(blockEntity);
                            else if(!list.contains(world.getBlockEntity(connectedChest)))list.add(blockEntity);
                        }
                    }else if (blockEntity instanceof Inventory) {
                        // Check if the chest is within the radius
                        if (player.getBlockPos().getSquaredDistance(blockPos) <= radius * radius) {
                            BlockPos connectedChest = BlockEntityUtil.getConnectedChestPos(player, blockPos);
                            if(connectedChest == null) list.add(blockEntity);
                            else if(!list.contains(world.getBlockEntity(connectedChest)))list.add(blockEntity);
                        }
                    }else if(isValidBlockEntity(blockPos, world)) {
                        list.add(blockEntity);
                    }
                }
            }
        }
        return list;
    }

    public static boolean isValidBlockEntity(BlockPos blockPos, World world) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);
        return blockEntity instanceof Inventory || ItemStorage.SIDED.find(world, blockPos, null) != null;
    }

    public static List<BlockEntity> sortByDistance(List<BlockEntity> blockPosList, ServerPlayerEntity player) {
        // Sort the blockPosList based on distance to the player
        blockPosList.sort(Comparator.comparingDouble(pos -> pos.getPos().getSquaredDistance(player.getX(), player.getY(), player.getZ())));
        return blockPosList;
    }

    public static ArrayList<DecodedBlockEntity> sortByItemCount(ArrayList<DecodedBlockEntity> decodedBlockEntities, Item item) {
        decodedBlockEntities.sort(Comparator.comparingInt(chest -> -chest.items().getOrDefault(item, 0)));
        return decodedBlockEntities;
    }
}
