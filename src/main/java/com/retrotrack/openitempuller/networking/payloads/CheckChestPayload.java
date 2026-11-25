package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import com.retrotrack.openitempuller.util.decoding.NbtCoder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

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
        int clampedRadius = Mth.clamp(radius, 1, ItemPuller.CONFIG.getInteger("radius"));
        List<BlockEntity> blockEntityList = BlockEntitySearchUtil.findSortedBlockEntitiesAroundPlayer(
                context.player().level(), context.player(), clampedRadius);

        ServerPlayNetworking.send(context.player(), new OpenPullItemScreenPayload(
                NbtCoder.encode(blockEntityList, context.player()), clampedRadius));
    }
}
