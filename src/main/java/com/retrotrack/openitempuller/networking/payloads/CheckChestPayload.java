package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import com.retrotrack.openitempuller.util.decoding.NbtCoder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public record CheckChestPayload(int radius)  implements CustomPayload {
    public static final CustomPayload.Id<CheckChestPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID, "check_chests"));
    public static final PacketCodec<RegistryByteBuf, CheckChestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, CheckChestPayload::radius,
            CheckChestPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }


    public void receiveServer(ServerPlayNetworking.Context context) {
        int clampedRadius = MathHelper.clamp(radius, 1, ItemPuller.CONFIG.getInteger("radius"));
        List<BlockEntity> blockEntityList = BlockEntitySearchUtil.findSortedBlockEntitiesAroundPlayer(
                context.player().getServerWorld(), context.player(), clampedRadius);

        ServerPlayNetworking.send(context.player(), new OpenPullItemScreenPayload(
                NbtCoder.encode(blockEntityList, context.player()), clampedRadius));
    }
}
