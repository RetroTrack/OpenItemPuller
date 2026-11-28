package com.retrotrack.openitempuller.networking;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * An item together with its data components (enchantments, custom name, shulker contents, ...), as sent over
 * the wire. It is written as a count-1 {@link ItemStack}; a blank (air) variant cannot be encoded.
 */
public final class VariantCodec {
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemVariant> CODEC =
            ItemStack.STREAM_CODEC.map(ItemVariant::of, ItemVariant::toStack);

    private VariantCodec() {}
}
