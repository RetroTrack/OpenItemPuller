package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.gui.screen.SettingsScreen;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public class OpenSettingsScreenS2CPacket {
    public static void receive(MinecraftClient client, ClientPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender sender) {
        int radius = buf.readInt();
        client.execute(() -> MinecraftClient.getInstance().setScreen(new SettingsScreen(client.currentScreen, radius)));
    }
}
