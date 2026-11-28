package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import com.retrotrack.openitempuller.util.RateLimiter;
import com.retrotrack.openitempuller.util.decoding.NbtCoder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

/** @param radius the radius this player <em>asks for</em>; the server clamps it to its own limit */
public record CheckChestPayload(int radius) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CheckChestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "check_chests"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CheckChestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CheckChestPayload::radius,
            CheckChestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void receiveServer(ServerPlayNetworking.Context context) {
        if (!RateLimiter.CHECK.tryAcquire(context.player())) {
            context.player().sendSystemMessage(
                    Component.translatable("openitempuller.message.rate_limited").withStyle(ChatFormatting.RED), true);
            return;
        }
        try {
            int maxRadius = ItemPuller.serverConfig().maxRadius();
            int clampedRadius = Mth.clamp(radius, 1, maxRadius);
            List<BlockEntity> blockEntityList = BlockEntitySearchUtil.findSortedBlockEntitiesAroundPlayer(
                    context.player().level(), context.player(), clampedRadius);

            NbtCoder.Encoded encoded = NbtCoder.encode(blockEntityList, context.player());
            ServerPlayNetworking.send(context.player(), new OpenPullItemScreenPayload(
                    encoded.variants(), encoded.chests(), maxRadius));
        } catch (Exception e) {
            // An exception in a network handler must never kick the player: log it and tell them instead.
            ItemPuller.LOGGER.error("Failed to build chest list for {}", context.player().getName().getString(), e);
            context.player().sendSystemMessage(
                    Component.translatable("openitempuller.message.internal_error").withStyle(ChatFormatting.RED), true);
        }
    }
}
