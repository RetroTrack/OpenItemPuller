package com.retrotrack.openitempuller.util;

import com.retrotrack.openitempuller.util.InventoryRoom.Slot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryRoomTest {
    private static int maxStack(String item) {
        return switch (item) {
            case "pearl" -> 16;
            case "sword" -> 1;
            default -> 64;
        };
    }

    private static List<Slot<String>> slots(int count, String item, int amount) {
        List<Slot<String>> list = new ArrayList<>();
        for (int k = 0; k < count; k++) list.add(new Slot<>(item, amount));
        return list;
    }

    private static InventoryRoom<String> room(List<Slot<String>> slots) {
        return new InventoryRoom<>(slots, InventoryRoomTest::maxStack);
    }

    private static InventoryRoom<String> emptyRoom(int size) {
        return room(slots(size, null, 0));
    }

    @Test
    void emptyInventoryHoldsFullStacks() {
        assertEquals(36 * 64, emptyRoom(36).insert("stone", 5000));
        assertEquals(36 * 16, emptyRoom(36).insert("pearl", 5000));
        assertEquals(36, emptyRoom(36).insert("sword", 100));
    }

    @Test
    void everythingFitsWhenThereIsRoom() {
        assertEquals(100, emptyRoom(36).insert("stone", 100));
    }

    @Test
    void nothingWantedOrNoSlotsMeansNothingFits() {
        assertEquals(0, emptyRoom(36).insert("stone", 0));
        assertEquals(0, emptyRoom(0).insert("stone", 10));
    }

    @Test
    void topsUpMatchingStacksBeforeUsingEmptySlots() {
        List<Slot<String>> slots = slots(3, null, 0);
        slots.set(0, new Slot<>("stone", 60));
        slots.set(1, new Slot<>("dirt", 64)); // full, other item
        assertEquals(4 + 64, room(slots).insert("stone", 100));
        assertEquals(64, room(slots).insert("gravel", 100)); // only the empty slot
    }

    @Test
    void fullInventoryTakesNothing() {
        assertEquals(0, room(slots(36, "dirt", 64)).insert("stone", 10));
    }

    @Test
    void oneFreeSpotPerMatchingStack() {
        assertEquals(36, room(slots(36, "stone", 63)).insert("stone", 1000));
    }

    @Test
    void differentItemsCompeteForTheSameEmptySlots() {
        InventoryRoom<String> room = emptyRoom(2);
        assertEquals(100, room.insert("stone", 100)); // takes both slots
        assertEquals(0, room.insert("dirt", 10));
    }

    @Test
    void insertingTheSameItemAgainReusesItsStack() {
        InventoryRoom<String> room = emptyRoom(2);
        room.insert("stone", 10);
        assertEquals(60, room.insert("stone", 60));
    }
}
