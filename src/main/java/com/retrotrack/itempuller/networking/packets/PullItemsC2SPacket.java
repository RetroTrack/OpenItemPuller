package com.retrotrack.itempuller.networking.packets;

import com.retrotrack.itempuller.networking.ModMessages;
import com.retrotrack.itempuller.util.ChestFinder;
import com.retrotrack.itempuller.util.decoding.DecodedChest;
import com.retrotrack.itempuller.util.decoding.NbtChestCoder;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class PullItemsC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            NbtCompound compound = buf.readNbt();
            if (compound == null) return;

            ArrayList<DecodedChest> decodedChests = NbtChestCoder.decode(compound.getCompound("buf"));
            int size = compound.getInt("size");
            for (int i = 0; i < size; i++) {
                NbtCompound child = compound.getCompound("chest_" + i);
                if (child == null) continue;

                DecodedChest decodedChest = decodedChests.get(i);
                BlockPos pos = decodedChest.pos();
                ChestBlockEntity chestBlockEntity = ChestFinder.checkChestBlockEntity(player.getServerWorld(), pos);
                if (chestBlockEntity == null) continue;

                Inventory chestInventory = HopperBlockEntity.getInventoryAt(player.getServerWorld(), chestBlockEntity.getPos());
                if (chestInventory == null) continue;

                Item item = Registries.ITEM.get(new Identifier(child.getString("item")));
                int itemCount = child.getInt("item_count");
                int itemsTransferred = 0;

                // Iterate through the chest inventory
                for (int j = 0; j < chestInventory.size(); j++) {
                    ItemStack chestInventoryStack = chestInventory.getStack(j);

                    // If the stack matches the item we're looking for
                    if (chestInventoryStack.getItem() == item) {
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
        });
    }
}
