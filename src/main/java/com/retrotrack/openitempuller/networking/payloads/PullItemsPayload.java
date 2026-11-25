package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.util.BlockEntityUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public record PullItemsPayload(CompoundTag compound) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PullItemsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "pull_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PullItemsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, PullItemsPayload::compound,
            PullItemsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void receiveServer(ServerPlayNetworking.Context context) {
        try {
            int totalTransferred = BlockEntityUtil.retrieveItemsBlockEntity(context.player(), compound);
            if (totalTransferred > 0) {
                // second argument true = action bar (overlay) message
                context.player().sendSystemMessage(
                        Component.translatable("openitempuller.message.pull_to_target.success", totalTransferred)
                                .withStyle(ChatFormatting.GREEN), true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
