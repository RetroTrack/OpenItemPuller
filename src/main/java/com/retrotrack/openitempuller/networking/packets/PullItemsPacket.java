package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.util.BlockEntityUtil;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public class PullItemsPacket implements FabricPacket {
    public static final PacketType<PullItemsPacket> TYPE = PacketType.create(
            new Identifier(MOD_ID, "pull_items"),
            PullItemsPacket::new
    );
    public final NbtCompound compound;

    public PullItemsPacket(NbtCompound nbtCompound) {
        this.compound = nbtCompound;
    }

    public PullItemsPacket(PacketByteBuf buf) {
        this(buf.readNbt());
    }

    public static void receiveServer(PullItemsPacket packet, ServerPlayerEntity player, PacketSender sender) {
        try {
            int totalTransferred = BlockEntityUtil.retrieveItemsBlockEntity(player, packet.compound);
            if (totalTransferred > 0) {
                player.sendMessage(Text.translatable("openitempuller.message.pull_to_target.success", totalTransferred).formatted(Formatting.GREEN), true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void write(PacketByteBuf buf) {
        buf.writeNbt(this.compound);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

}
