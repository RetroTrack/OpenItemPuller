package com.retrotrack.openitempuller.util.decoding;

import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import com.retrotrack.openitempuller.util.BlockEntityUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class NbtCoder {
    // Encodes chest data into a PacketByteBuf
    public static NbtCompound encode(List<BlockEntity> blockEntityList, ServerPlayerEntity player) {
        // Create a NbtCompound to hold the encoded data
        NbtCompound nbtCompound = new NbtCompound();

        // Write the size of the chest block entity list to the compound
        int size = 0;

        // Iterate through each chest block entity in the list
        for (BlockEntity blockEntity : blockEntityList) {
            // Get the inventory of the chest
            Inventory inventory = HopperBlockEntity.getInventoryAt(player.getWorld(), blockEntity.getPos());

            // Check if inventory exists, otherwise use check if block entity contains storage
            if (inventory != null) {
                // Create a new NbtCompound to store data for this chest
                NbtCompound blockEntityNbt = new NbtCompound();
                // Lists to store unique items and their counts
                ArrayList<Item> uniqueItems = new ArrayList<>();
                ArrayList<Integer> itemCounts = new ArrayList<>();

                // Iterate through each slot in the chest inventory
                IntStream.range(0, inventory.size())
                        // Filter out empty slots and duplicate items
                        .filter(slotIndex -> {
                            Item itemInSlot = inventory.getStack(slotIndex).getItem();
                            return inventory.getStack(slotIndex) != null && !uniqueItems.contains(itemInSlot) && !inventory.getStack(slotIndex).isEmpty();
                        })
                        // Process each valid slot
                        .forEach(slotIndex -> {
                            Item itemInSlot = inventory.getStack(slotIndex).getItem();
                            uniqueItems.add(itemInSlot);
                            itemCounts.add(inventory.count(itemInSlot));
                        });

                // Write chest name and position to the chest NBT compound
                String chestName = getName(player, blockEntity);

                blockEntityNbt.putString("chest_name", chestName);
                blockEntityNbt.putIntArray("pos", new int[]{blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ()});

                // Create a child NbtCompound to store item data
                NbtCompound itemList = getItemList(uniqueItems, itemCounts);
                blockEntityNbt.put("item_list", itemList);
                nbtCompound.put("chest_" + size, blockEntityNbt);

                size++;
            } else if (BlockEntitySearchUtil.isValidBlockEntity(blockEntity.getPos(), player.getWorld())) {

                // Create a new NbtCompound to store data for this chest
                NbtCompound blockEntityNbt = new NbtCompound();

                // Lists to store unique items and their counts
                ArrayList<Item> uniqueItems = new ArrayList<>();
                ArrayList<Integer> itemCounts = new ArrayList<>();

                // Get the storage from the block entity
                Storage<ItemVariant> storage = BlockEntityUtil.getStorage(blockEntity.getPos(), player.getWorld(), player, false);
                if (storage == null) continue;

                // Iterate through the storage
                storage.iterator().forEachRemaining(view -> {
                    // Check if the slot contains an item
                    if (!view.isResourceBlank()) {
                        ItemVariant itemVariant = view.getResource();
                        Item item = itemVariant.toStack().getItem(); // Convert to Item

                        // If the item is not already in the unique list, add it
                        if (!uniqueItems.contains(item)) {
                            uniqueItems.add(item);
                            itemCounts.add((int) view.getAmount()); // Record the count of the item
                        }
                    }
                });

                // Write chest name and position to the chest NBT compound
                String chestName = getName(player, blockEntity);
                blockEntityNbt.putString("chest_name", chestName);
                blockEntityNbt.putIntArray("pos", new int[]{
                        blockEntity.getPos().getX(),
                        blockEntity.getPos().getY(),
                        blockEntity.getPos().getZ()
                });

                // Create a child NbtCompound to store item data
                NbtCompound itemList = getItemList(uniqueItems, itemCounts);
                blockEntityNbt.put("item_list", itemList);
                nbtCompound.put("chest_" + size, blockEntityNbt);
                size++;
            }
        }
        nbtCompound.putInt("chest_list_size", size);
        //Return compound
        return nbtCompound;
    }

    private static String getName(ServerPlayerEntity player, BlockEntity blockEntity) {
        String chestName;
        if (blockEntity instanceof Nameable) {
            chestName = ((Nameable) blockEntity).getName().getString();
        } else {
            chestName = "translatable: " + player.getWorld().getBlockState(blockEntity.getPos()).getBlock().getTranslationKey();
        }
        return chestName;
    }

    private static NbtCompound getItemList(ArrayList<Item> uniqueItems, ArrayList<Integer> itemCounts) {
        NbtCompound itemList = new NbtCompound();
        // Write the size of the item list to the item NBT compound
        itemList.putInt("item_list_size", uniqueItems.size());

        // Iterate through each unique item in the storage
        for (int itemIndex = 0; itemIndex < uniqueItems.size(); itemIndex++) {
            Item item = uniqueItems.get(itemIndex);
            Identifier itemId = Registries.ITEM.getId(item); // Get the identifier of the item
            itemList.putString("item_" + itemIndex, itemId.toString()); // Add item identifier to the NBT
            itemList.putInt("item_" + itemIndex + "_amount", itemCounts.get(itemIndex)); // Add item count to the NBT
        }

        return itemList;
    }

    public static ArrayList<DecodedBlockEntity>  decode(NbtCompound nbtCompound) {
        ArrayList<DecodedBlockEntity> decodedBlockEntities = new ArrayList<>();
        if (nbtCompound == null) return decodedBlockEntities;

        //Check chests
        Optional<Integer> chestListSize = nbtCompound.getInt("chest_list_size");
        if (chestListSize.isEmpty()) {
            return decodedBlockEntities; // Return empty list if no chests found
        }
        for (int chestIndex = 0; chestIndex < chestListSize.get(); chestIndex++) {
            // Nbt Compounds
            Optional<NbtCompound> chestNbt = nbtCompound.getCompound("chest_" + chestIndex);
            Optional<NbtCompound> itemList = chestNbt.flatMap(nbt -> nbt.getCompound("item_list"));

            // Chest info
            if (chestNbt.get().getString("chest_name").isEmpty()) continue; // Skip if chest or item list is not present
            String chestName = chestNbt.get().getString("chest_name").get();
            if(chestNbt.get().getIntArray("pos").isEmpty()) continue; // Skip if position is not present
            BlockPos pos = new BlockPos(chestNbt.get().getIntArray("pos").get()[0], chestNbt.get().getIntArray("pos").get()[1], chestNbt.get().getIntArray("pos").get()[2]);

            // Items
            if(itemList.get().getInt("item_list_size").isEmpty()) continue; // Skip if item list size is not present
            int itemListSize = itemList.get().getInt("item_list_size").get();

            HashMap<Item, Integer> map = new HashMap<>();
            for (int itemIndex = 0; itemIndex < itemListSize; itemIndex++) {
                if (itemList.get().getString("item_" + itemIndex).isPresent() || itemList.get().getInt("item_" + itemIndex + "_amount").isEmpty()) {
                    continue; // Skip if item or amount is not present
                }
                Item item = Registries.ITEM.get(Identifier.of(itemList.get().getString("item_" + itemIndex).get()));
                map.put(item, itemList.get().getInt("item_" + itemIndex + "_amount").get());
            }

            decodedBlockEntities.add(new DecodedBlockEntity(chestName, pos, map));
        }
        return decodedBlockEntities;
    }

}
