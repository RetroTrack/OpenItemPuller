package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.gui.screen.SettingsScreen;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public class OpenSettingsScreenPacket implements FabricPacket {
    public static final PacketType<OpenSettingsScreenPacket> TYPE = PacketType.create(
            Identifier.of(MOD_ID, "open_settings_screen"),
            OpenSettingsScreenPacket::new
    );
    public final int radius;

    public OpenSettingsScreenPacket(int serverRadius) {
        this.radius = serverRadius;
    }

    public OpenSettingsScreenPacket(PacketByteBuf buf) {
        this(buf.readInt());
    }
    public static void receiveClient(OpenSettingsScreenPacket packet, ClientPlayerEntity player, PacketSender sender) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new SettingsScreen(client.currentScreen, packet.radius));
    }
    public static void receiveServer(OpenSettingsScreenPacket packet, ServerPlayerEntity player, PacketSender sender) {
        OpenSettingsScreenPacket openPullItemScreenPacket = new OpenSettingsScreenPacket(CONFIG.getInteger("radius"));
        ServerPlayNetworking.send(player, openPullItemScreenPacket);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.radius);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
