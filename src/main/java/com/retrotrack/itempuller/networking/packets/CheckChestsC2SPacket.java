package com.retrotrack.itempuller.networking.packets;

import com.retrotrack.itempuller.networking.ModMessages;
import com.retrotrack.itempuller.util.ChestFinder;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CheckChestsC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            int radius = MathHelper.clamp(buf.getInt(0), 1, 128);
            List<ChestBlockEntity> chestBlockEntityList = ChestFinder.findChestsAroundPlayer(player.getServerWorld(), player, radius); // Replace 128 with config value once done!

            // Create a single PacketByteBuf to hold all the data
            PacketByteBuf sendBuf = PacketByteBufs.create();
            sendBuf.writeInt(chestBlockEntityList.size()); // Write the number of chest entities

            // Serialize all data into the single PacketByteBuf
            for (ChestBlockEntity chestBlockEntity : chestBlockEntityList) {
                Inventory inventory = HopperBlockEntity.getInventoryAt(player.getServerWorld(), chestBlockEntity.getPos());
                if (inventory == null) continue;

                ArrayList<Item> items = new ArrayList<>();
                ArrayList<Integer> itemCounts = new ArrayList<>();

                IntStream.range(0, inventory.size()).filter(i -> inventory.getStack(i) != null && !items.contains(inventory.getStack(i).getItem()) && !inventory.getStack(i).isEmpty()).forEach(i -> {
                    items.add(inventory.getStack(i).getItem());
                    itemCounts.add(inventory.count(inventory.getStack(i).getItem()));
                });

                // Write chest name, position and item amount
                sendBuf.writeString(chestBlockEntity.getName().getString()); // 0
                sendBuf.writeInt(items.size()); // 1
                sendBuf.writeBlockPos(chestBlockEntity.getPos()); // 2

                // Write items and their counts -> 3,4,5 etc
                for (int i = 0; i < items.size(); i++) {
                    sendBuf.writeItemStack(items.get(i).getDefaultStack());
                    sendBuf.writeInt(itemCounts.get(i));
                }
            }

            // Send the single PacketByteBuf containing all data
            ServerPlayNetworking.send(player, ModMessages.OPEN_PULL_ITEM_SCREEN, sendBuf);
        });


    }
}
