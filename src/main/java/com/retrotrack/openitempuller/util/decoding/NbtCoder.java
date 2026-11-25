package com.retrotrack.openitempuller.util.decoding;

import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import com.retrotrack.openitempuller.util.BlockEntityUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NbtCoder {
    // Encodes block entity (chest) data into a CompoundTag
    public static CompoundTag encode(List<BlockEntity> blockEntityList, ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        int size = 0;

        for (BlockEntity blockEntity : blockEntityList) {
            BlockPos blockPos = blockEntity.getBlockPos();
            Container container = HopperBlockEntity.getContainerAt(player.level(), blockPos);

            ArrayList<Item> uniqueItems = new ArrayList<>();
            ArrayList<Integer> itemCounts = new ArrayList<>();

            if (container != null) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    var stack = container.getItem(slot);
                    if (stack.isEmpty()) continue;
                    Item item = stack.getItem();
                    if (uniqueItems.contains(item)) continue;
                    uniqueItems.add(item);
                    itemCounts.add(container.countItem(item));
                }
            } else if (BlockEntitySearchUtil.isValidBlockEntity(blockPos, player.level())) {
                Storage<ItemVariant> storage = BlockEntityUtil.getStorage(blockPos, player.level(), player, false);
                if (storage == null) continue;

                storage.iterator().forEachRemaining(view -> {
                    if (!view.isResourceBlank()) {
                        Item item = view.getResource().toStack().getItem();
                        if (!uniqueItems.contains(item)) {
                            uniqueItems.add(item);
                            itemCounts.add((int) view.getAmount());
                        }
                    }
                });
            } else {
                continue;
            }

            CompoundTag blockEntityNbt = new CompoundTag();
            blockEntityNbt.putString("chest_name", getName(player, blockEntity));
            blockEntityNbt.putIntArray("pos", new int[]{blockPos.getX(), blockPos.getY(), blockPos.getZ()});
            blockEntityNbt.put("item_list", getItemList(uniqueItems, itemCounts));
            root.put("chest_" + size, blockEntityNbt);
            size++;
        }
        root.putInt("chest_list_size", size);
        return root;
    }

    private static String getName(ServerPlayer player, BlockEntity blockEntity) {
        if (blockEntity instanceof Nameable nameable) {
            return nameable.getName().getString();
        }
        return "translatable: " + player.level().getBlockState(blockEntity.getBlockPos()).getBlock().getDescriptionId();
    }

    private static CompoundTag getItemList(ArrayList<Item> uniqueItems, ArrayList<Integer> itemCounts) {
        CompoundTag itemList = new CompoundTag();
        itemList.putInt("item_list_size", uniqueItems.size());

        for (int i = 0; i < uniqueItems.size(); i++) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(uniqueItems.get(i));
            itemList.putString("item_" + i, itemId.toString());
            itemList.putInt("item_" + i + "_amount", itemCounts.get(i));
        }
        return itemList;
    }

    public static ArrayList<DecodedBlockEntity> decode(CompoundTag tag) {
        ArrayList<DecodedBlockEntity> decoded = new ArrayList<>();
        if (tag == null) return decoded;

        // 26.x NBT getters return Optional; use the *Or / *OrEmpty variants
        int chestListSize = tag.getIntOr("chest_list_size", 0);
        for (int chestIndex = 0; chestIndex < chestListSize; chestIndex++) {
            CompoundTag chestNbt = tag.getCompoundOrEmpty("chest_" + chestIndex);
            CompoundTag itemList = chestNbt.getCompoundOrEmpty("item_list");

            String chestName = chestNbt.getStringOr("chest_name", "");
            int[] p = chestNbt.getIntArray("pos").orElse(new int[]{0, 0, 0});
            BlockPos pos = new BlockPos(p[0], p[1], p[2]);

            int itemListSize = itemList.getIntOr("item_list_size", 0);
            HashMap<Item, Integer> map = new HashMap<>();
            for (int i = 0; i < itemListSize; i++) {
                Identifier id = Identifier.parse(itemList.getStringOr("item_" + i, "minecraft:air"));
                Item item = BuiltInRegistries.ITEM.getValue(id);
                map.put(item, itemList.getIntOr("item_" + i + "_amount", 0));
            }
            decoded.add(new DecodedBlockEntity(chestName, pos, map));
        }
        return decoded;
    }
}
