package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.gui.screen.PullItemScreen;
import com.retrotrack.openitempuller.util.decoding.NbtChestCoder;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public class OpenPullItemScreenPacket implements FabricPacket {
    public static final PacketType<OpenPullItemScreenPacket> TYPE = PacketType.create(
            new Identifier(MOD_ID, "open_pull_screen"),
            OpenPullItemScreenPacket::new
    );

    public final NbtCompound encodedChests;
    public final int serverRadius;

    public OpenPullItemScreenPacket(NbtCompound decodedChests, int serverRadius) {
        this.encodedChests = decodedChests;
        this.serverRadius = serverRadius;
    }

    public OpenPullItemScreenPacket(PacketByteBuf buf) {
        this(buf.readNbt(), buf.readInt());
    }

    public static void receiveClient(OpenPullItemScreenPacket packet, ClientPlayerEntity player, PacketSender sender) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new PullItemScreen(client.currentScreen, NbtChestCoder.decode(packet.encodedChests), packet.serverRadius));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeNbt(this.encodedChests);
        buf.writeInt(this.serverRadius);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
