package com.retrotrack.itempuller.util;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ChestConnectionChecker {
    public static BlockPos getConnectedChestPos(ServerPlayerEntity player, BlockPos chestPos) {
        World world = player.getServerWorld();
        ChestBlockEntity chestBlockEntity = (ChestBlockEntity) world.getBlockEntity(chestPos);
        if (chestBlockEntity == null || !(world.getBlockState(chestPos).getBlock() instanceof ChestBlock)) {
            return null; // Return null if the block is not a chest
        }

        ChestType chestType = world.getBlockState(chestPos).get(ChestBlock.CHEST_TYPE);
        if (chestType.equals(ChestType.SINGLE)) {
            return null; // Return null if the chest is single
        }

        Direction facing = world.getBlockState(chestPos).get(Properties.HORIZONTAL_FACING);
        BlockPos connectedChestPos;
        switch (facing) {
            case NORTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.EAST) : chestPos.offset(Direction.WEST);
            case EAST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.SOUTH) : chestPos.offset(Direction.NORTH);
            case SOUTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.WEST) : chestPos.offset(Direction.EAST);
            case WEST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.NORTH) : chestPos.offset(Direction.SOUTH);
            default -> {
                return null; // Return null if facing direction is unknown
            }
        }

        if (world.getBlockState(connectedChestPos).getBlock() instanceof ChestBlock) {
            return connectedChestPos;
        }
        return null; // Return null if no connected chest found
    }

}

