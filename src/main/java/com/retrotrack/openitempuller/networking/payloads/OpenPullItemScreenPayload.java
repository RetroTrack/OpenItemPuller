package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.gui.screen.PullItemScreen;
import com.retrotrack.openitempuller.util.decoding.NbtCoder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public record OpenPullItemScreenPayload(CompoundTag encodedChests, int serverRadius) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenPullItemScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "open_pull_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPullItemScreenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, OpenPullItemScreenPayload::encodedChests,
            ByteBufCodecs.INT, OpenPullItemScreenPayload::serverRadius,
            OpenPullItemScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public void receiveClient(ClientPlayNetworking.Context context) {
        context.client().gui.setScreen(new PullItemScreen(context.client().gui.screen(), NbtCoder.decode(encodedChests), serverRadius));
    }
}
