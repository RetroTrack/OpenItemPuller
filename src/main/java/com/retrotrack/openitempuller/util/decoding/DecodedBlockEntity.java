package com.retrotrack.openitempuller.util.decoding;

import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;

public record DecodedBlockEntity(String name, BlockPos pos,
                                 HashMap<Item, Integer> items) {
}
