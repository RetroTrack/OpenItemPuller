package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.util.BlockEntityUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public record PullItemsPayload(NbtCompound compound) implements CustomPayload {
    public static final CustomPayload.Id<PullItemsPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID, "pull_items"));
    public static final PacketCodec<RegistryByteBuf, PullItemsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.NBT_COMPOUND, PullItemsPayload::compound, PullItemsPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }


    public void receiveServer(ServerPlayNetworking.Context context) {
        try {
            int totalTransferred = BlockEntityUtil.retrieveItemsBlockEntity(context.player(), compound);
            if (totalTransferred > 0) {
                context.player().sendMessage(Text.translatable("openitempuller.message.pull_to_target.success", totalTransferred).formatted(Formatting.GREEN), true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
