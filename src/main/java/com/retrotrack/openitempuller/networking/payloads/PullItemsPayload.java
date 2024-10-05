package com.retrotrack.openitempuller.networking.payloads;

import com.retrotrack.openitempuller.util.ChestFinder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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
            int size = compound.getInt("size");
            for (int i = 0; i < size; i++) {
                NbtCompound child = compound.getCompound("chest_id_" + i);
                if (child == null) continue;
                BlockPos pos = new BlockPos(child.getIntArray("pos")[0], child.getIntArray("pos")[1], child.getIntArray("pos")[2]);

                LockableContainerBlockEntity chestBlockEntity = ChestFinder.checkLockableContainerBlockEntity(context.player().getServerWorld(), pos);
                if (chestBlockEntity == null) continue;

                Inventory chestInventory = HopperBlockEntity.getInventoryAt(context.player().getServerWorld(), chestBlockEntity.getPos());
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
                        context.player().getInventory().offerOrDrop(copiedStack);

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
}
