package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.networking.ModMessages;
import com.retrotrack.openitempuller.util.ChestFinder;
import com.retrotrack.openitempuller.util.decoding.NbtChestCoder;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class CheckChestsC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            int radius = MathHelper.clamp(buf.getInt(0),
                    1,
                    ItemPuller.CONFIG.getInteger("radius"));
            List<LootableContainerBlockEntity> chestBlockEntityList = ChestFinder.findSortedLootableContainerBlockEntitiesAroundPlayer(player.getServerWorld(), player, radius); // Replace 128 with config value once done!
            ServerPlayNetworking.send(player, ModMessages.OPEN_PULL_ITEM_SCREEN, NbtChestCoder.encode(chestBlockEntityList, player).writeInt(ItemPuller.CONFIG.getInteger("radius")));
        });


    }
}
