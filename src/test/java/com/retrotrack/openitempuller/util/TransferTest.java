package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.MinecraftTestBootstrap;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The item transfer the pull is built on, run on real Fabric item storages around in-memory containers:
 * what is moved, what is left behind, and what happens when the target is full.
 * (Chunk loading, permissions and the player's own inventory need a running world and are not covered here.)
 */
class TransferTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.init();
    }

    // --- helpers -----------------------------------------------------------------------------------------

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** Fills every slot with a full stack of dirt, so nothing else fits there. */
    private static SimpleContainer fullOfDirt(int slots) {
        SimpleContainer container = new SimpleContainer(slots);
        for (int slot = 0; slot < slots; slot++) container.setItem(slot, stack(Items.DIRT, 64));
        return container;
    }

    /** The same storages the mod uses: items are taken from {@link BlockEntityUtil#EXTRACT_SIDE} and put in unsided. */
    private static int move(Container from, Container to, Item item, int amount) {
        Storage<ItemVariant> source = ContainerStorage.of(from, BlockEntityUtil.EXTRACT_SIDE);
        Storage<ItemVariant> target = ContainerStorage.of(to, null);
        return BlockEntityUtil.transferFromStorage(source, target, ItemVariant.of(item), amount);
    }

    /** A furnace as far as the pull cares: input (0), fuel (1) and output (2), with the sided rules of a hopper. */
    private static final class FakeFurnace extends SimpleContainer implements WorldlyContainer {
        FakeFurnace() {
            super(3);
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.DOWN ? new int[]{2, 1} : new int[]{0};
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
            return slot != 2;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
            return slot == 2 || (slot == 1 && stack.is(Items.BUCKET));
        }
    }

    // --- amounts -----------------------------------------------------------------------------------------

    @Test
    void movesOnlyTheRequestedAmount() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 64));
        SimpleContainer target = new SimpleContainer(27);

        assertEquals(10, move(source, target, Items.STONE, 10));
        assertEquals(54, count(source, Items.STONE));
        assertEquals(10, count(target, Items.STONE));
    }

    @Test
    void takesFromSeveralStacks() {
        SimpleContainer source = new SimpleContainer(27);
        for (int slot = 0; slot < 3; slot++) source.setItem(slot, stack(Items.STONE, 64));
        SimpleContainer target = new SimpleContainer(27);

        assertEquals(150, move(source, target, Items.STONE, 150));
        assertEquals(42, count(source, Items.STONE));
        assertEquals(150, count(target, Items.STONE));
    }

    @Test
    void asksForMoreThanThereIsAndGetsWhatIsThere() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 5));
        SimpleContainer target = new SimpleContainer(27);

        assertEquals(5, move(source, target, Items.STONE, 1000));
        assertEquals(0, count(source, Items.STONE));
    }

    @Test
    void otherItemsInTheSourceStayPut() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 10));
        source.setItem(1, stack(Items.DIAMOND, 3));
        SimpleContainer target = new SimpleContainer(27);

        move(source, target, Items.STONE, 64);
        assertEquals(3, count(source, Items.DIAMOND));
        assertEquals(0, count(target, Items.DIAMOND));
    }

    // --- a target that is full or nearly full --------------------------------------------------------------

    @Test
    void aFullTargetTakesNothingAndTheSourceKeepsEverything() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 64));
        SimpleContainer target = fullOfDirt(27);

        assertEquals(0, move(source, target, Items.STONE, 64));
        assertEquals(64, count(source, Items.STONE));
    }

    @Test
    void movesOnlyWhatFitsAndLeavesTheRestBehind() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 64));
        source.setItem(1, stack(Items.STONE, 36));
        SimpleContainer target = fullOfDirt(27);
        target.setItem(26, ItemStack.EMPTY); // exactly one free slot

        assertEquals(64, move(source, target, Items.STONE, 100));
        assertEquals(36, count(source, Items.STONE));
        assertEquals(64, count(target, Items.STONE));
    }

    @Test
    void topsUpAnExistingStackInTheTarget() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 64));
        SimpleContainer target = fullOfDirt(27);
        target.setItem(0, stack(Items.STONE, 60)); // room for 4 more, nothing else free

        assertEquals(4, move(source, target, Items.STONE, 64));
        assertEquals(60, count(source, Items.STONE));
        assertEquals(64, count(target, Items.STONE));
    }

    // --- stack sizes -------------------------------------------------------------------------------------

    @Test
    void unstackableItemsNeedOneSlotEach() {
        SimpleContainer source = new SimpleContainer(27);
        for (int slot = 0; slot < 5; slot++) source.setItem(slot, stack(Items.DIAMOND_SWORD, 1));
        SimpleContainer target = fullOfDirt(27);
        target.setItem(0, ItemStack.EMPTY);
        target.setItem(1, ItemStack.EMPTY); // two free slots

        assertEquals(2, move(source, target, Items.DIAMOND_SWORD, 5));
        assertEquals(3, count(source, Items.DIAMOND_SWORD));
    }

    @Test
    void itemsThatStackTo16FillASlotOnlyToSixteen() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.ENDER_PEARL, 16));
        source.setItem(1, stack(Items.ENDER_PEARL, 16));
        source.setItem(2, stack(Items.ENDER_PEARL, 8));
        SimpleContainer target = fullOfDirt(27);
        target.setItem(0, ItemStack.EMPTY); // one free slot

        assertEquals(16, move(source, target, Items.ENDER_PEARL, 40));
        assertEquals(24, count(source, Items.ENDER_PEARL));
    }

    // --- variants ----------------------------------------------------------------------------------------

    @Test
    void movesOnlyTheExactVariantNotARenamedOne() {
        ItemStack renamed = stack(Items.STONE, 20);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Special"));
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 10));
        source.setItem(1, renamed);
        SimpleContainer target = new SimpleContainer(27);

        assertEquals(10, move(source, target, Items.STONE, 100));  // only the plain stack
        assertEquals(20, source.getItem(1).getCount());            // the renamed one was not touched
        assertEquals(10, count(target, Items.STONE));
    }

    @Test
    void aRenamedVariantCanBeMovedOnItsOwn() {
        ItemStack renamed = stack(Items.STONE, 20);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Special"));
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 10));
        source.setItem(1, renamed);
        SimpleContainer target = new SimpleContainer(27);

        long moved = BlockEntityUtil.transferFromStorage(
                ContainerStorage.of(source, BlockEntityUtil.EXTRACT_SIDE), ContainerStorage.of(target, null),
                ItemVariant.of(renamed), 100);
        assertEquals(20, moved);
        assertEquals(10, count(source, Items.STONE));
    }

    // --- missing storages --------------------------------------------------------------------------------

    @Test
    void aMissingSourceOrTargetMovesNothing() {
        SimpleContainer container = new SimpleContainer(27);
        container.setItem(0, stack(Items.STONE, 10));
        Storage<ItemVariant> storage = ContainerStorage.of(container, null);

        assertEquals(0, BlockEntityUtil.transferFromStorage(null, storage, ItemVariant.of(Items.STONE), 10));
        assertEquals(0, BlockEntityUtil.transferFromStorage(storage, null, ItemVariant.of(Items.STONE), 10));
        assertEquals(10, count(container, Items.STONE));
    }

    // --- furnaces ----------------------------------------------------------------------------------------

    @Test
    void aFurnaceGivesUpItsOutputButNotItsInputOrFuel() {
        FakeFurnace furnace = new FakeFurnace();
        furnace.setItem(0, stack(Items.IRON_ORE, 8));
        furnace.setItem(1, stack(Items.COAL, 5));
        furnace.setItem(2, stack(Items.IRON_INGOT, 3));
        SimpleContainer target = new SimpleContainer(27);

        assertEquals(3, move(furnace, target, Items.IRON_INGOT, 64));
        assertEquals(0, move(furnace, target, Items.IRON_ORE, 64));
        assertEquals(0, move(furnace, target, Items.COAL, 64));
        assertEquals(8, count(furnace, Items.IRON_ORE));
        assertEquals(5, count(furnace, Items.COAL));
    }

    @Test
    void anEmptyBucketInTheFuelSlotCanBeTaken() {
        FakeFurnace furnace = new FakeFurnace();
        furnace.setItem(1, stack(Items.BUCKET, 1));
        SimpleContainer target = new SimpleContainer(27);

        assertEquals(1, move(furnace, target, Items.BUCKET, 64));
    }

    // --- double chests -----------------------------------------------------------------------------------

    @Test
    void aDoubleChestActsAsOneInventoryWhenTakingItemsOut() {
        SimpleContainer left = new SimpleContainer(27);
        SimpleContainer right = new SimpleContainer(27);
        left.setItem(0, stack(Items.STONE, 64));
        right.setItem(0, stack(Items.STONE, 64));
        CompoundContainer doubleChest = new CompoundContainer(left, right);
        SimpleContainer target = new SimpleContainer(27);

        assertEquals(100, move(doubleChest, target, Items.STONE, 100));
        assertEquals(28, count(left, Items.STONE) + count(right, Items.STONE));
    }

    @Test
    void aDoubleChestAsTargetUsesBothHalves() {
        SimpleContainer left = fullOfDirt(27);
        SimpleContainer right = new SimpleContainer(27); // all the room is in the second half
        CompoundContainer doubleChest = new CompoundContainer(left, right);
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, stack(Items.STONE, 64));

        assertEquals(64, move(source, doubleChest, Items.STONE, 64));
        assertEquals(64, count(right, Items.STONE));
    }
}
