package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.gui.screen.SettingsScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Objects;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public record OpenSettingsScreenPayload(int radius) implements CustomPayload {
    public static final CustomPayload.Id<OpenSettingsScreenPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID, "open_settings_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenSettingsScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, OpenSettingsScreenPayload::radius, OpenSettingsScreenPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }


    public void receiveServer(ServerPlayNetworking.Context context) {
        ServerPlayNetworking.send(context.player(), new OpenSettingsScreenPayload(CONFIG.getInteger("radius")));
    }

    public void receiveClient(ClientPlayNetworking.Context context) {
        context.client().setScreen(new SettingsScreen(context.client().currentScreen, radius));
    }
}
