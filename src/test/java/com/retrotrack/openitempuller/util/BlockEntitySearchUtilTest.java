package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlockEntitySearchUtilTest {

    private ServerPlayerEntity mockPlayer;
    private ServerWorld mockWorld;

    @BeforeEach
    public void setup() {
        mockPlayer = mock(ServerPlayerEntity.class, RETURNS_DEEP_STUBS);
        mockWorld = mock(ServerWorld.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void testSortByDistance() {
        BlockEntity be1 = mock(BlockEntity.class);
        BlockEntity be2 = mock(BlockEntity.class);

        when(be1.getPos()).thenReturn(new BlockPos(0, 64, 0));
        when(be2.getPos()).thenReturn(new BlockPos(10, 64, 0));

        when(mockPlayer.getX()).thenReturn(0.0);
        when(mockPlayer.getY()).thenReturn(64.0);
        when(mockPlayer.getZ()).thenReturn(0.0);

        List<BlockEntity> list = new ArrayList<>();
        list.add(be2);
        list.add(be1);

        List<BlockEntity> sorted = BlockEntitySearchUtil.sortByDistance(list, mockPlayer);
        assertEquals(be1, sorted.get(0));
        assertEquals(be2, sorted.get(1));
    }

    @Test
    public void testSortByItemCount() {
        Item testItem = mock(Item.class);
        DecodedBlockEntity dbe1 = mock(DecodedBlockEntity.class);
        DecodedBlockEntity dbe2 = mock(DecodedBlockEntity.class);

        HashMap<Item, Integer> map1 = new HashMap<>();
        map1.put(testItem, 5);

        HashMap<Item, Integer> map2 = new HashMap<>();
        map2.put(testItem, 10);

        when(dbe1.items()).thenReturn(map1);
        when(dbe2.items()).thenReturn(map2);


        ArrayList<DecodedBlockEntity> list = new ArrayList<>();
        list.add(dbe1);
        list.add(dbe2);

        var sorted = BlockEntitySearchUtil.sortByItemCount(list, testItem);
        assertEquals(dbe2, sorted.get(0));
        assertEquals(dbe1, sorted.get(1));
    }

    @Test
    public void testIsUnlocked_lootPresent() {
        LootableContainerBlockEntity lootable = mock(LootableContainerBlockEntity.class);
        RegistryKey<LootTable> fakeKey = RegistryKey.of(RegistryKeys.LOOT_TABLE,
                Identifier.of("minecraft", "chests/simple_dungeon"));

        when(lootable.getLootTable()).thenReturn(fakeKey);
        when(mockPlayer.getWorld().getBlockEntity(new BlockPos(0, 0, 0))).thenReturn(lootable);

        assertNull(BlockEntitySearchUtil.isUnlocked(mockPlayer, new BlockPos(0, 0, 0)));
    }

    @Test
    public void testIsUnlocked_noLoot() {
        LootableContainerBlockEntity lootable = mock(LootableContainerBlockEntity.class);

        when(lootable.getLootTable()).thenReturn(null);
        when(mockPlayer.getWorld().getBlockEntity(new BlockPos(0, 0, 0))).thenReturn(lootable);

        assertEquals(lootable, BlockEntitySearchUtil.isUnlocked(mockPlayer, new BlockPos(0, 0, 0)));
    }

    @Test
    public void testIsValidBlockEntity_withInventory() {
        Inventory inventory = mock(Inventory.class);
        World world = mock(World.class);
        BlockPos pos = new BlockPos(1, 1, 1);

        when(world.getBlockEntity(pos)).thenReturn((BlockEntity) inventory);
        assertTrue(BlockEntitySearchUtil.isValidBlockEntity(pos, world));
    }

    // Additional tests could tackle ItemStorage.SIDED.find(...) with static mocking (if needed)
}
