package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.networking.ModMessages;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class OpenSettingsScreenC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> ServerPlayNetworking.send(player, ModMessages.OPEN_SETTINGS_SCREEN_S2C, PacketByteBufs.create().writeInt(ItemPuller.CONFIG.getInteger("radius"))));
    }
}
