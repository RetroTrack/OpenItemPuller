package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.gui.screen.SettingsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public record OpenSettingsScreenPayload(int radius) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenSettingsScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "open_settings_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSettingsScreenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OpenSettingsScreenPayload::radius,
            OpenSettingsScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void receiveServer(ServerPlayNetworking.Context context) {
        ServerPlayNetworking.send(context.player(), new OpenSettingsScreenPayload(CONFIG.getInteger("radius")));
    }

    @Environment(EnvType.CLIENT)
    public void receiveClient(ClientPlayNetworking.Context context) {
        context.client().gui.setScreen(new SettingsScreen(context.client().gui.screen(), radius));
    }
}
