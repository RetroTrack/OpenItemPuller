package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockEntitySearchUtil {

    public static List<BlockEntity> findSortedBlockEntitiesAroundPlayer(ServerLevel world, ServerPlayer player, int radius) {
        return sortByDistance(findBlockEntitiesAroundPlayer(world, player, radius), player);
    }

    public static List<BlockEntity> findBlockEntitiesAroundPlayer(ServerLevel world, ServerPlayer player, int radius) {
        List<BlockEntity> list = new ArrayList<>();

        int chunkRadius = (int) Math.ceil((double) radius / 16);
        ChunkPos playerChunkPos = new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkPos.x() + dx, playerChunkPos.z() + dz);
                // getChunkNow only returns already-loaded chunks: never loads or generates anything
                LevelChunk chunk = world.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
                if (chunk == null) continue;

                for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
                    // one rule for every kind of block entity, including non-Container item storages
                    if (!PullValidator.canAccess(player, blockPos, radius)) continue;
                    BlockEntity blockEntity = world.getBlockEntity(blockPos);
                    if (blockEntity == null) continue;
                    if (blockEntity instanceof Container) {
                        addUnlessDoubleChestHalf(list, world, player, blockPos, blockEntity);
                    } else {
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

    public static ArrayList<DecodedBlockEntity> sortByItemCount(ArrayList<DecodedBlockEntity> decodedBlockEntities, ItemVariant variant) {
        decodedBlockEntities.sort(Comparator.comparingInt(chest -> -chest.items().getOrDefault(variant, 0)));
        return decodedBlockEntities;
    }
}
