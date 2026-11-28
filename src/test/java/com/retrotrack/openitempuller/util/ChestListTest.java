package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.MinecraftTestBootstrap;
import com.retrotrack.openitempuller.config.ClientConfig.PriorityType;
import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which chests are listed for an item, and in which order. */
class ChestListTest {
    // Must run before the fields below: they touch Items, which needs the registries to exist already.
    // (A @BeforeAll is too late: static fields are initialised first.)
    static {
        MinecraftTestBootstrap.init();
    }

    private static final ItemVariant STONE = ItemVariant.of(Items.STONE);
    private static final ItemVariant DIAMOND = ItemVariant.of(Items.DIAMOND);

    private static DecodedBlockEntity chest(int x, ItemVariant variant, int count) {
        HashMap<ItemVariant, Integer> items = new HashMap<>();
        items.put(variant, count);
        return new DecodedBlockEntity(Component.literal("chest " + x), new BlockPos(x, 64, 0), items);
    }

    private static List<Integer> xs(List<DecodedBlockEntity> chests) {
        return chests.stream().map(c -> c.pos().getX()).toList();
    }

    @Test
    void onlyChestsWithTheItemAreListed() {
        ArrayList<DecodedBlockEntity> all = new ArrayList<>(List.of(chest(1, STONE, 5), chest(2, DIAMOND, 9), chest(3, STONE, 7)));
        assertEquals(List.of(1, 3), xs(BlockEntityUtil.getChestsWithItem(STONE, all, PriorityType.RANGE)));
    }

    @Test
    void rangePriorityKeepsTheOrderTheServerSent() {
        ArrayList<DecodedBlockEntity> all = new ArrayList<>(List.of(chest(1, STONE, 5), chest(2, STONE, 50), chest(3, STONE, 7)));
        assertEquals(List.of(1, 2, 3), xs(BlockEntityUtil.getChestsWithItem(STONE, all, PriorityType.RANGE)));
    }

    @Test
    void itemCountPriorityPutsTheFullestChestFirst() {
        ArrayList<DecodedBlockEntity> all = new ArrayList<>(List.of(chest(1, STONE, 5), chest(2, STONE, 50), chest(3, STONE, 7)));
        assertEquals(List.of(2, 3, 1), xs(BlockEntityUtil.getChestsWithItem(STONE, all, PriorityType.ITEM_COUNT)));
    }

    @Test
    void noChestHasTheItem() {
        ArrayList<DecodedBlockEntity> all = new ArrayList<>(List.of(chest(1, DIAMOND, 5)));
        assertEquals(List.of(), xs(BlockEntityUtil.getChestsWithItem(STONE, all, PriorityType.RANGE)));
    }
}
