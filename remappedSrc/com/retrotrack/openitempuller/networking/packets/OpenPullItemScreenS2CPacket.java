package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.gui.screen.PullItemScreen;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public class OpenPullItemScreenS2CPacket {
    public static void receive(MinecraftClient client, ClientPlayNetworkHandler handler,
                               PacketByteBuf buf, PacketSender sender) {
        client.execute(() -> MinecraftClient.getInstance().setScreen(new PullItemScreen(client.currentScreen, buf)));
    }
}
