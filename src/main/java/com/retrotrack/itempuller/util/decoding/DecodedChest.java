package com.retrotrack.itempuller.util.decoding;

import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public record DecodedChest(String name, BlockPos pos,
                           ArrayList<Item> uniqueItems,
                           ArrayList<Integer> itemCounts) {
}
