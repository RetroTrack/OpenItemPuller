package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.gui.screen.PullItemScreen;
import com.retrotrack.openitempuller.util.decoding.NbtChestCoder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public record OpenPullItemScreenPayload(NbtCompound encodedChests, int serverRadius) implements CustomPayload {
    public static final CustomPayload.Id<OpenPullItemScreenPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID, "open_pull_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenPullItemScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.NBT_COMPOUND, OpenPullItemScreenPayload::encodedChests,
            PacketCodecs.INTEGER, OpenPullItemScreenPayload::serverRadius,
            OpenPullItemScreenPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    @Environment(EnvType.CLIENT)
    public void receiveClient(ClientPlayNetworking.Context context) {
        context.client().setScreen(new PullItemScreen(context.client().currentScreen, NbtChestCoder.decode(encodedChests), serverRadius));
    }
}
