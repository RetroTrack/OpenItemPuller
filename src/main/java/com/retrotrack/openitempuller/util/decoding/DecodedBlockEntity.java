package com.retrotrack.openitempuller.util.decoding;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashMap;

/**
 * @param name  translatable for default names (resolved in the client's language), literal for custom names
 * @param items total per item variant: an enchanted or renamed item is a different key than a plain one
 */
public record DecodedBlockEntity(Component name, BlockPos pos,
                                 HashMap<ItemVariant, Integer> items) {
}
