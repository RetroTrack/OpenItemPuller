package com.retrotrack.itempuller.util.decoding;

import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;

public record DecodedChest(String name, BlockPos pos,
                           HashMap<Item, Integer> items) {
}
