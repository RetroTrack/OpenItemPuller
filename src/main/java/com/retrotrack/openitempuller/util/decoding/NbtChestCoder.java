package com.retrotrack.openitempuller.util.decoding;

import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

public class NbtChestCoder {
    // Encodes chest data into a PacketByteBuf
    public static NbtCompound encode(List<LockableContainerBlockEntity> lockableContainerBlockEntityList, ServerPlayerEntity player) {
        // Create a NbtCompound to hold the encoded data
        NbtCompound nbtCompound = new NbtCompound();

        // Write the size of the chest block entity list to the compound
        nbtCompound.putInt("chest_list_size", lockableContainerBlockEntityList.size());

        // Iterate through each chest block entity in the list
        for (int chestIndex = 0; chestIndex < lockableContainerBlockEntityList.size(); chestIndex++) {
            LockableContainerBlockEntity chestBlockEntity = lockableContainerBlockEntityList.get(chestIndex);
            // Get the inventory of the chest
            Inventory chestInventory = HopperBlockEntity.getInventoryAt(player.getServerWorld(), chestBlockEntity.getPos());

            // If the chest inventory is null, skip to the next chest
            if (chestInventory == null) continue;

            // Create a new NbtCompound to store data for this chest
            NbtCompound chestNbt = new NbtCompound();
            // Lists to store unique items and their counts
            ArrayList<Item> uniqueItems = new ArrayList<>();
            ArrayList<Integer> itemCounts = new ArrayList<>();

            // Iterate through each slot in the chest inventory
            IntStream.range(0, chestInventory.size())
                    // Filter out empty slots and duplicate items
                    .filter(slotIndex -> {
                        Item itemInSlot = chestInventory.getStack(slotIndex).getItem();
                        return chestInventory.getStack(slotIndex) != null && !uniqueItems.contains(itemInSlot) && !chestInventory.getStack(slotIndex).isEmpty();
                    })
                    // Process each valid slot
                    .forEach(slotIndex -> {
                        Item itemInSlot = chestInventory.getStack(slotIndex).getItem();
                        uniqueItems.add(itemInSlot);
                        itemCounts.add(chestInventory.count(itemInSlot));
                    });

            // Write chest name and position to the chest NBT compound
            chestNbt.putString("chest_name", chestBlockEntity.getName().getString());
            chestNbt.putIntArray("pos", new int[]{chestBlockEntity.getPos().getX(), chestBlockEntity.getPos().getY(), chestBlockEntity.getPos().getZ()});

            // Create a child NbtCompound to store item data
            NbtCompound itemList = new NbtCompound();
            // Write the size of the item list to the item NBT compound
            itemList.putInt("item_list_size", uniqueItems.size());

            // Iterate through each unique item in the chest inventory
            for (int itemIndex = 0; itemIndex < uniqueItems.size(); itemIndex++) {
                Item item = uniqueItems.get(itemIndex);
                // Get the identifier of the item
                Identifier itemId = Registries.ITEM.getId(item);
                // Write the item identifier and its count to the item NBT compound
                itemList.putString("item_" + itemIndex, itemId.toString());
                itemList.putInt("item_" + itemIndex + "_amount", itemCounts.get(itemIndex));
            }

            // Add the item NBT compound to the chest NBT compound
            chestNbt.put("item_list", itemList);
            // Add the chest NBT compound to the main compound with a unique key
            nbtCompound.put("chest_" + chestIndex, chestNbt);
        }

        //Return compound
        return nbtCompound;
    }

    public static ArrayList<DecodedChest> decode(NbtCompound nbtCompound) {
        ArrayList<DecodedChest> decodedChests = new ArrayList<>();
        if (nbtCompound == null) return decodedChests;

        //Check chests
        int chestListSize = nbtCompound.getInt("chest_list_size");
        for (int chestIndex = 0; chestIndex < chestListSize; chestIndex++) {
            // Nbt Compounds
            NbtCompound chestNbt = nbtCompound.getCompound("chest_" + chestIndex);
            NbtCompound itemList = chestNbt.getCompound("item_list");

            // Chest info
            String chestName = chestNbt.getString("chest_name");
            BlockPos pos = new BlockPos(chestNbt.getIntArray("pos")[0], chestNbt.getIntArray("pos")[1], chestNbt.getIntArray("pos")[2]);

            // Items
            int itemListSize = itemList.getInt("item_list_size");

            HashMap<Item, Integer> map = new HashMap<>();
            for (int itemIndex = 0; itemIndex < itemListSize; itemIndex++) {
                Item item = Registries.ITEM.get(Identifier.of(itemList.getString("item_" + itemIndex)));
                map.put(item, itemList.getInt("item_" + itemIndex + "_amount"));
            }

            decodedChests.add(new DecodedChest(chestName, pos, map));
        }
        return decodedChests;
    }

}
