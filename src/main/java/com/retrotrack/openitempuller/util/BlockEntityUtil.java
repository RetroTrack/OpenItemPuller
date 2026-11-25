package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;

public class BlockEntityUtil {

    public static BlockPos getConnectedChestPos(ServerPlayer player, BlockPos chestPos) {
        Level world = player.level();
        if (!(world.getBlockEntity(chestPos) instanceof ChestBlockEntity)) return null;
        if (!(world.getBlockState(chestPos).getBlock() instanceof ChestBlock)) return null;

        ChestType chestType = world.getBlockState(chestPos).getValue(ChestBlock.TYPE);
        if (chestType.equals(ChestType.SINGLE)) return null;

        Direction facing = world.getBlockState(chestPos).getValue(ChestBlock.FACING);
        BlockPos connectedChestPos;
        switch (facing) {
            case NORTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.EAST) : chestPos.relative(Direction.WEST);
            case EAST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.SOUTH) : chestPos.relative(Direction.NORTH);
            case SOUTH -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.WEST) : chestPos.relative(Direction.EAST);
            case WEST -> connectedChestPos = chestType.equals(ChestType.LEFT) ? chestPos.relative(Direction.NORTH) : chestPos.relative(Direction.SOUTH);
            default -> {
                return null;
            }
        }

        if (world.getBlockState(connectedChestPos).getBlock() instanceof ChestBlock) return connectedChestPos;
        return null;
    }

    public static ArrayList<DecodedBlockEntity> getChestsWithItem(Item item, ArrayList<DecodedBlockEntity> decodedBlockEntities) {
        ArrayList<DecodedBlockEntity> collect = decodedBlockEntities.stream()
                .filter(chest -> chest.items().containsKey(item))
                .collect(Collectors.toCollection(ArrayList::new));
        if (CONFIG.getInteger("priority_type") == 1) return BlockEntitySearchUtil.sortByItemCount(collect, item);
        return collect;
    }

    public static int retrieveItemsBlockEntity(ServerPlayer player, CompoundTag compound) {
        int size = compound.getIntOr("size", 0);
        int totalItemsTransferred = 0;

        boolean fromBlock = compound.getBooleanOr("from_block", false);
        BlockPos targetPos = null;
        if (fromBlock) {
            int[] t = compound.getIntArray("target_pos").orElse(null);
            if (t == null || t.length < 3) fromBlock = false;
            else targetPos = new BlockPos(t[0], t[1], t[2]);
        }

        for (int i = 0; i < size; i++) {
            Optional<CompoundTag> childOpt = compound.getCompound("chest_id_" + i);
            if (childOpt.isEmpty()) continue;
            CompoundTag child = childOpt.get();
            int[] s = child.getIntArray("source_pos").orElse(null);
            if (s == null || s.length < 3) continue;
            BlockPos sourcePos = new BlockPos(s[0], s[1], s[2]);

            BlockEntity blockEntity = BlockEntitySearchUtil.isUnlocked(player, sourcePos);
            if (blockEntity == null) continue;

            totalItemsTransferred += transfer(player, child.getStringOr("item", "minecraft:air"),
                    child.getIntOr("item_count", 0), sourcePos, targetPos, fromBlock);
        }
        return totalItemsTransferred;
    }

    public static int transfer(ServerPlayer player, String idString, int count, BlockPos sourcePos, BlockPos targetPos, boolean fromBlock) {
        Identifier id = Identifier.parse(idString);
        Item item = BuiltInRegistries.ITEM.getValue(id);
        int itemsTransferred = 0;
        Level level = player.level();

        Container sourceInventory = HopperBlockEntity.getContainerAt(level, sourcePos);
        Container targetInventory = null;

        if (fromBlock) {
            targetInventory = HopperBlockEntity.getContainerAt(level, targetPos);

            if (targetInventory == null) {
                Storage<ItemVariant> targetStorage = getStorage(targetPos, level, null, false);
                if (targetStorage != null) {
                    Storage<ItemVariant> sourceStorage = getStorage(sourcePos, level, null, false);
                    if (sourceStorage != null) {
                        itemsTransferred += transferFromStorage(sourceStorage, targetStorage, item, count);
                        return itemsTransferred;
                    }
                }
            }
        }

        if (!fromBlock || targetInventory == null) {
            targetInventory = player.getInventory();
        }

        if (sourceInventory == null) {
            Storage<ItemVariant> sourceStorage = getStorage(sourcePos, level, null, false);
            Storage<ItemVariant> targetStorage = null;

            if (fromBlock) {
                targetStorage = getStorage(targetPos, level, null, false);
            }
            if (targetStorage == null) {
                targetStorage = getStorage(null, null, player, true);
            }

            itemsTransferred += transferFromStorage(sourceStorage, targetStorage, item, count);
        }

        if (sourceInventory != null || targetInventory != null) {
            if (fromBlock && targetInventory instanceof Inventory) {
                player.sendSystemMessage(Component.translatable("openitempuller.message.pull_to_target.error").withStyle(ChatFormatting.RED), true);
            }
            itemsTransferred += transferFromInventory(sourceInventory, targetInventory, item, count);
        }

        return itemsTransferred;
    }

    @Nullable
    public static Storage<ItemVariant> getStorage(@Nullable BlockPos pos, @Nullable Level world, @Nullable ServerPlayer player, boolean fallback) {
        Storage<ItemVariant> storage = null;

        if (pos != null && world != null) {
            storage = ItemStorage.SIDED.find(world, pos, null);
        }

        if (storage == null && player != null && fallback) {
            storage = PlayerInventoryStorage.of(player);
            player.sendSystemMessage(Component.translatable("openitempuller.message.pull_to_target.error").withStyle(ChatFormatting.RED), true);
        }

        return storage;
    }

    public static int transferFromInventory(Container source, Container target, Item item, int maxAmount) {
        if (source == null || target == null) return 0;
        int itemsTransferred = 0;

        for (int j = 0; j < source.getContainerSize(); j++) {
            ItemStack sourceStack = source.getItem(j);
            if (!sourceStack.getItem().equals(item)) continue;

            int transferAmount = Math.min(maxAmount - itemsTransferred, sourceStack.getCount());
            if (transferAmount <= 0) continue;

            ItemStack splitStack = sourceStack.split(transferAmount);

            if (target instanceof Inventory playerInventory) {
                // add() moves what fits; whatever does not fit goes back into the chest
                playerInventory.add(splitStack);
                int leftover = splitStack.getCount();
                if (leftover > 0) {
                    sourceStack.grow(leftover);
                    itemsTransferred += transferAmount - leftover;
                    break; // inventory is full
                }
                itemsTransferred += transferAmount;
                continue;
            }

            for (int k = 0; k < target.getContainerSize(); k++) {
                ItemStack existingStack = target.getItem(k);

                if (!target.canTakeItem(target, k, splitStack)) continue;

                if (existingStack.isEmpty()) {
                    target.setItem(k, splitStack);
                    itemsTransferred += splitStack.getCount();
                    splitStack = ItemStack.EMPTY;
                    break;
                } else if (canStackAddMore(existingStack, splitStack, target)) {
                    int availableSpace = existingStack.getMaxStackSize() - existingStack.getCount();
                    int mergeAmount = Math.min(splitStack.getCount(), availableSpace);
                    existingStack.grow(mergeAmount);
                    splitStack.shrink(mergeAmount);
                    itemsTransferred += mergeAmount;

                    if (splitStack.isEmpty()) {
                        break;
                    }
                }
            }

            if (!splitStack.isEmpty()) {
                sourceStack.grow(splitStack.getCount());
                break;
            }

            if (itemsTransferred >= maxAmount) break;
        }
        return itemsTransferred;
    }

    private static boolean canStackAddMore(ItemStack existingStack, ItemStack stack, Container container) {
        return !existingStack.isEmpty() && ItemStack.isSameItemSameComponents(existingStack, stack)
                && existingStack.isStackable() && existingStack.getCount() < container.getMaxStackSize(existingStack);
    }

    public static int transferFromStorage(Storage<ItemVariant> source, Storage<ItemVariant> target, Item item, int maxAmount) {
        if (source == null || target == null) return 0;
        return (int) StorageUtil.move(source, target, iv -> iv.getItem().equals(item), maxAmount, null);
    }
}
