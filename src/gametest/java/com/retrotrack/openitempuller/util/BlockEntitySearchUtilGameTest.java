package com.retrotrack.openitempuller.util;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class BlockEntitySearchUtilGameTest {

    @GameTest(templateName = "openitempuller:block_entity_search_util_test")
    public void testFindSortedBlockEntitiesAroundPlayer(TestContext context) {
        ServerWorld world = (ServerWorld) helper.getWorld();
        BlockPos playerPos = new BlockPos(1, 2, 1);

        // Spawn or find a fake player at playerPos
        ServerPlayerEntity fakePlayer = con.spawn(ServerPlayerEntity.class, playerPos);

        // Setup: place some block entities in the world around the player
        // Example: place a chest at (2,2,1)
        BlockPos chestPos = new BlockPos(2, 2, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        BlockEntity blockEntity = world.getBlockEntity(chestPos);

        // Wait for the block entity to load
        helper.succeedWhen(() -> blockEntity != null);

        // Call the method under test
        List<BlockEntity> foundEntities = BlockEntitySearchUtil.findSortedBlockEntitiesAroundPlayer(world, fakePlayer, 10);

        // Assert the list contains our chest block entity
        helper.succeedWhen(() -> foundEntities.contains(blockEntity));

        // Extra: assert sorting correctness
        boolean sorted = true;
        for (int i = 0; i < foundEntities.size() - 1; i++) {
            double dist1 = foundEntities.get(i).getPos().getSquaredDistance(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ());
            double dist2 = foundEntities.get(i + 1).getPos().getSquaredDistance(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ());
            if (dist1 > dist2) {
                sorted = false;
                break;
            }
        }
        if (!sorted) {
            throw new AssertionError("Block entities are not sorted by distance!");
        }
    }
}
