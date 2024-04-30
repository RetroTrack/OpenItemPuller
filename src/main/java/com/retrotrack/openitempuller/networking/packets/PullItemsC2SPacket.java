package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.util.ChestFinder;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PullItemsC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            try {
                NbtCompound compound = buf.readNbt();
                if (compound == null) return;
                int size = compound.getInt("size");
                for (int i = 0; i < size; i++) {
                    NbtCompound child = compound.getCompound("chest_id_" + i);
                    if (child == null) continue;
                    BlockPos pos = new BlockPos(child.getIntArray("pos")[0], child.getIntArray("pos")[1], child.getIntArray("pos")[2]);

                    LootableContainerBlockEntity chestBlockEntity = ChestFinder.checkLootableContainerBlockEntity(player.getServerWorld(), pos);
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
