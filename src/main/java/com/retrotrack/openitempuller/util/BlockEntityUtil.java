package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;

public class BlockEntityUtil {

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


    public static ArrayList<DecodedBlockEntity> getChestsWithItem(Item item, ArrayList<DecodedBlockEntity> decodedBlockEntities) {
        ArrayList<DecodedBlockEntity> collect = decodedBlockEntities.stream().filter(chest -> chest.items().containsKey(item)).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<DecodedBlockEntity> collect2;
        if(CONFIG.getInteger("priority_type") == 1) collect2 = BlockEntitySearchUtil.sortByItemCount(collect, item);
        else collect2 = collect;
        return collect2;
    }

    public static int retrieveItemsBlockEntity(ServerPlayerEntity player, NbtCompound compound) {
        int size = compound.getInt("size");
        int totalItemsTransferred = 0;

        boolean fromBlock = compound.getBoolean("from_block");
        BlockPos targetPos = null;
        if(fromBlock) targetPos = new BlockPos(compound.getIntArray("target_pos")[0], compound.getIntArray("target_pos")[1], compound.getIntArray("target_pos")[2]);

        for (int i = 0; i < size; i++) {
            NbtCompound child = compound.getCompound("chest_id_" + i);
            if (child == null) continue;
            BlockPos sourcePos = new BlockPos(child.getIntArray("source_pos")[0], child.getIntArray("source_pos")[1], child.getIntArray("source_pos")[2]);

            BlockEntity blockEntity = BlockEntitySearchUtil.isUnlocked(player, sourcePos);
            if (blockEntity == null) continue;

            totalItemsTransferred += transfer(player, child.getString("item"), child.getInt("item_count"), sourcePos, targetPos, fromBlock);
        }
        return totalItemsTransferred;
    }

    public static int transfer(ServerPlayerEntity player, String idString, int count, BlockPos sourcePos, BlockPos targetPos, boolean fromBlock) {
        // Get item from identifier gathered from nbt compound
        String[] split = idString.split(":");
        Identifier id = Identifier.of(split[0], split[1]);
        Item item = Registries.ITEM.get(id);
        int itemsTransferred = 0;

        // Get inventories required for transferring items
        Inventory sourceInventory = HopperBlockEntity.getInventoryAt(player.getServerWorld(), sourcePos);
        Inventory targetInventory = null;

        if (fromBlock) {
            targetInventory = HopperBlockEntity.getInventoryAt(player.getServerWorld(), targetPos);

            // If no valid inventory is found, check for storage
            if (targetInventory == null) {
                Storage<ItemVariant> targetStorage = getStorage(targetPos, player.getServerWorld(), null, false);

                if (targetStorage != null) {
                    Storage<ItemVariant> sourceStorage = getStorage(sourcePos, player.getServerWorld(), null, false);

                    if (sourceStorage != null) {
                        itemsTransferred += transferFromStorage(sourceStorage, targetStorage, item, count);
                        return itemsTransferred; // Exit early since the transfer is handled by storage
                    }
                }
            }
        }

        // Fallback to player inventory if necessary
        if (!fromBlock || targetInventory == null) {
            targetInventory = player.getInventory();
        }

        // Check block if contains inventory or else check if it has a storage
        if (sourceInventory == null) {
            Storage<ItemVariant> sourceStorage = getStorage(sourcePos, player.getServerWorld(), null, false);
            Storage<ItemVariant> targetStorage = null;

            if (fromBlock) {
                targetStorage = getStorage(targetPos, player.getServerWorld(), null, false);
            }
            if (targetStorage == null) {
                targetStorage = getStorage(null, null, player, true);
            }

            itemsTransferred += transferFromStorage(sourceStorage, targetStorage, item, count);
        }

        if (sourceInventory != null || targetInventory != null) {
            if (fromBlock && targetInventory instanceof PlayerInventory) {
                player.sendMessage(Text.translatable("openitempuller.message.pull_to_target.error").formatted(Formatting.RED), true);
            }
            itemsTransferred += transferFromInventory(sourceInventory, targetInventory, item, count);
        }

        return itemsTransferred;
    }

    @Nullable
    public static Storage<ItemVariant> getStorage(@Nullable BlockPos pos, @Nullable World world, @Nullable ServerPlayerEntity player, boolean fallback) {
        Storage<ItemVariant> storage = null;

        if (pos != null && world != null) {
            storage = ItemStorage.SIDED.find(world, pos, null);
        }

        if (storage == null && player != null && fallback) {
            storage = PlayerInventoryStorage.of(player);
            player.sendMessage(Text.translatable("openitempuller.message.pull_to_target.error").formatted(Formatting.RED), true);
        }

        return storage;
    }


    public static int transferFromInventory(Inventory source, Inventory target, Item item, int maxAmount) {
        if (source == null || target == null) return 0;
        int itemsTransferred = 0;

        // Iterate through the source inventory
        for (int j = 0; j < source.size(); j++) {
            ItemStack sourceStack = source.getStack(j);
            if (!sourceStack.getItem().equals(item)) continue;

            // Calculate the transfer amount
            int transferAmount = Math.min(maxAmount - itemsTransferred, sourceStack.getCount());
            if (transferAmount <= 0) continue; // No more items to transfer

            // Split the stack to the amount needed for the transfer
            ItemStack splitStack = sourceStack.split(transferAmount);

            //Check if inventory type is a player inventory, then offer or drop or continue
            if(target instanceof PlayerInventory) {
                ((PlayerInventory) target).offerOrDrop(splitStack);
                itemsTransferred += transferAmount;
                continue;
            }

            // Attempt to add the split stack to the target inventory
            for (int k = 0; k < target.size(); k++) {
                ItemStack existingStack = target.getStack(k);

                // Check if the slot can accept the item
                if (!target.canTransferTo(target, k, splitStack)) continue;

                if (existingStack.isEmpty()) {
                    // If the slot is empty, place the stack here
                    target.setStack(k, splitStack);
                    itemsTransferred += splitStack.getCount();
                    splitStack = ItemStack.EMPTY; // The stack is fully transferred
                    break;
                } else if (canStackAddMore(existingStack, splitStack, target)) {
                    // If the slot contains the same item, try to merge
                    int availableSpace = existingStack.getMaxCount() - existingStack.getCount();
                    int mergeAmount = Math.min(splitStack.getCount(), availableSpace);
                    existingStack.increment(mergeAmount);
                    splitStack.decrement(mergeAmount);
                    itemsTransferred += mergeAmount;

                    if (splitStack.isEmpty()) {
                        break; // The stack is fully transferred
                    }
                }
            }

            // If items remain in the split stack, return them to the source slot
            if (!splitStack.isEmpty()) {
                sourceStack.increment(splitStack.getCount()); // Return items to the original stack
                break; // Exit the loop as no more items can be transferred
            }

            // If all items are transferred, exit the loop
            if (itemsTransferred >= maxAmount) break;
        }
        return itemsTransferred;
    }

    private static boolean canStackAddMore(ItemStack existingStack, ItemStack stack, Inventory inventory) {
        return !existingStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(existingStack, stack) && existingStack.isStackable() && existingStack.getCount() < inventory.getMaxCount(existingStack);
    }


    public static int transferFromStorage(Storage<ItemVariant> source, Storage<ItemVariant> target, Item item, int maxAmount) {
        if (source == null || target == null) return 0;
        return (int) StorageUtil.move(
                source,
                target,
                iv -> iv.getItem().equals(item),
                maxAmount,
                null
        );

    }
}