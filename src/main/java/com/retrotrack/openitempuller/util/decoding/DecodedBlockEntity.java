package com.retrotrack.openitempuller.util.decoding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

import java.util.HashMap;

public record DecodedBlockEntity(String name, BlockPos pos,
                                 HashMap<Item, Integer> items) {
}
