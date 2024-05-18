package com.retrotrack.openitempuller.networking.packets;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.util.ChestFinder;
import com.retrotrack.openitempuller.util.decoding.NbtChestCoder;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public class PullItemsPacket implements FabricPacket {
    public static final PacketType<PullItemsPacket> TYPE = PacketType.create(
            new Identifier(MOD_ID, "pull_items"),
            PullItemsPacket::new
    );
    public final NbtCompound nbtCompound;

    public PullItemsPacket(NbtCompound nbtCompound) {
        this.nbtCompound = nbtCompound;
    }

    public PullItemsPacket(PacketByteBuf buf) {
        this(buf.readNbt());
    }

    public static void receiveServer(PullItemsPacket packet, ServerPlayerEntity player, PacketSender sender) {
        try {
            NbtCompound compound = packet.nbtCompound;
            if (compound == null) return;
            int size = compound.getInt("size");
            for (int i = 0; i < size; i++) {
                NbtCompound child = compound.getCompound("chest_id_" + i);
                if (child == null) continue;
                BlockPos pos = new BlockPos(child.getIntArray("pos")[0], child.getIntArray("pos")[1], child.getIntArray("pos")[2]);

                LockableContainerBlockEntity chestBlockEntity = ChestFinder.checkLockableContainerBlockEntity(player.getServerWorld(), pos);
                if (chestBlockEntity == null) continue;

                Inventory chestInventory = HopperBlockEntity.getInventoryAt(player.getServerWorld(), chestBlockEntity.getPos());
                if (chestInventory == null) continue;

                int itemCount = child.getInt("item_count");
                int itemsTransferred = 0;

                // Iterate through the chest inventory
                for (int j = 0; j < chestInventory.size(); j++) {
                    ItemStack chestInventoryStack = chestInventory.getStack(j);
                    // If the stack matches the item we're looking for
                    if (Registries.ITEM.getId(chestInventoryStack.getItem()).toString().equals(child.getString("item"))) {
                        // Calculate the transfer amount
                        int transferAmount = Math.min(itemCount - itemsTransferred, chestInventoryStack.getCount());
                        if (transferAmount <= 0) continue; // No more items to transfer

                        // Create a copy of the stack with the transfer amount
                        ItemStack copiedStack = chestInventoryStack.split(transferAmount);

                        // Offer the copied stack to the player's inventory or drop it
                        player.getInventory().offerOrDrop(copiedStack);

                        // Update the count of items transferred
                        itemsTransferred += transferAmount;

                        // If all items are transferred, exit the loop
                        if (itemsTransferred >= itemCount) break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void write(PacketByteBuf buf) {
        buf.writeNbt(this.nbtCompound);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

}
