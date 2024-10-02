package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.util.ChestFinder;
import com.retrotrack.openitempuller.util.decoding.NbtChestCoder;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public class CheckChestPacket implements FabricPacket {

    public static final PacketType<CheckChestPacket> TYPE = PacketType.create(
            Identifier.of(MOD_ID, "check_chests"),
            CheckChestPacket::new
    );
    public final int radius;

    public CheckChestPacket(int radius) {
        this.radius = radius;
    }

    public CheckChestPacket(PacketByteBuf buf) {
        this(buf.readInt());
    }

    public static void receiveServer(CheckChestPacket packet,ServerPlayerEntity player, PacketSender sender) {
        int radius = MathHelper.clamp(packet.radius, 1, ItemPuller.CONFIG.getInteger("radius"));
        List<LockableContainerBlockEntity> chestBlockEntityList = ChestFinder.findSortedLootableContainerBlockEntitiesAroundPlayer(player.getServerWorld(), player, radius);
        OpenPullItemScreenPacket openPullItemScreenPacket = new OpenPullItemScreenPacket(NbtChestCoder.encode(chestBlockEntityList, player), radius);
        ServerPlayNetworking.send(player, openPullItemScreenPacket);
    }
    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.radius);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
