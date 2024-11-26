package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.decoding.DecodedChest;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;

public class ChestUtil {

    public static BlockPos getConnectedChestPos(ServerPlayerEntity player, BlockPos chestPos) {
        World world = player.getServerWorld();
        if(!(world.getBlockEntity(chestPos) instanceof ChestBlockEntity)) return null;
        if (!(world.getBlockState(chestPos).getBlock() instanceof ChestBlock)) return null; // Return null if the block is not a chest

        ChestType chestType = world.getBlockState(chestPos).get(ChestBlock.CHEST_TYPE);
        if (chestType.equals(ChestType.SINGLE)) return null; // Return null if the chest is single

        Direction facing = world.getBlockState(chestPos).get(Properties.HORIZONTAL_FACING);
        BlockPos connectedChestPos;
        switch (facing) {
            case NORTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.EAST) : chestPos.offset(Direction.WEST);
            case EAST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.SOUTH) : chestPos.offset(Direction.NORTH);
            case SOUTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.WEST) : chestPos.offset(Direction.EAST);
            case WEST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.offset(Direction.NORTH) : chestPos.offset(Direction.SOUTH);
            default -> {
                return null; // Facing direction is unknown
            }
        }

        if (world.getBlockState(connectedChestPos).getBlock() instanceof ChestBlock) return connectedChestPos;
        return null; // Return null if no connected chest found
    }


    public static ArrayList<DecodedChest> getChestsWithItem(Item item, ArrayList<DecodedChest> decodedChests) {
        ArrayList<DecodedChest> collect = decodedChests.stream().filter(chest -> chest.items().containsKey(item)).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<DecodedChest> collect2;
        if(CONFIG.getInteger("priority_type") == 1) collect2 = ChestFinder.sortByItemCount(collect, item);
        else collect2 = collect;
        return collect2;
    }

    public static void retrieveItemsFromChest(ServerPlayerEntity player, NbtCompound compound) {
        int size = compound.getInt("size");
        for (int i = 0; i < size; i++) {
            NbtCompound child = compound.getCompound("chest_id_" + i);
            if (child == null) continue;
            BlockPos pos = new BlockPos(child.getIntArray("pos")[0], child.getIntArray("pos")[1], child.getIntArray("pos")[2]);

            LockableContainerBlockEntity chestBlockEntity = ChestFinder.checkLockableContainerBlockEntity(player.getServerWorld(), pos);
            if (chestBlockEntity == null) continue;

            Inventory chestInventory = HopperBlockEntity.getInventoryAt(player.getServerWorld(), chestBlockEntity.getPos());
            if (chestInventory == null) continue;

            int itemCount = child.getInt("item_count");
            int itemsTransferred = 0;

            // Iterate through the chest inventory
            for (int j = 0; j < chestInventory.size(); j++) {
                ItemStack chestInventoryStack = chestInventory.getStack(j);
                // If the stack matches the item we're looking for
                if (Registries.ITEM.getId(chestInventoryStack.getItem()).toString().equals(child.getString("item"))) {
                    // Calculate the transfer amount
                    int transferAmount = Math.min(itemCount - itemsTransferred, chestInventoryStack.getCount());
                    if (transferAmount <= 0) continue; // No more items to transfer

                    // Create a copy of the stack with the transfer amount
                    ItemStack copiedStack = chestInventoryStack.split(transferAmount);

                    // Offer the copied stack to the player's inventory or drop it
                    player.getInventory().offerOrDrop(copiedStack);

                    // Update the count of items transferred
                    itemsTransferred += transferAmount;

                    // If all items are transferred, exit the loop
                    if (itemsTransferred >= itemCount) break;
                }
            }
        }
    }
}

