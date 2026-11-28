package com.retrotrack.openitempuller.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class QuickAmountTest {
    private static final List<Integer> CHESTS = List.of(30, 64, 5);

    @Test
    void oneComesFromTheFirstChest() {
        assertArrayEquals(new int[]{1, 0, 0}, QuickAmount.distribute(CHESTS, 1));
    }

    @Test
    void aStackSpansChestsInListOrder() {
        assertArrayEquals(new int[]{30, 34, 0}, QuickAmount.distribute(CHESTS, 64));
    }

    @Test
    void exactTotalTakesEverything() {
        assertArrayEquals(new int[]{30, 64, 5}, QuickAmount.distribute(CHESTS, 99));
    }

    @Test
    void askingForMoreThanExistsTakesEverything() {
        assertArrayEquals(new int[]{30, 64, 5}, QuickAmount.distribute(CHESTS, 1000));
        assertArrayEquals(new int[]{30, 64, 5}, QuickAmount.all(CHESTS));
    }

    @Test
    void zeroOrNegativeTakesNothing() {
        assertArrayEquals(new int[]{0, 0, 0}, QuickAmount.distribute(CHESTS, 0));
        assertArrayEquals(new int[]{0, 0, 0}, QuickAmount.distribute(CHESTS, -5));
    }

    @Test
    void noChestsGivesNoAmounts() {
        assertArrayEquals(new int[]{}, QuickAmount.distribute(List.of(), 10));
    }

    @Test
    void negativeStockCountsAsEmpty() {
        assertArrayEquals(new int[]{0, 5}, QuickAmount.distribute(List.of(-3, 10), 5));
    }

    @Test
    void hugeCountsDoNotOverflow() {
        assertArrayEquals(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE},
                QuickAmount.all(List.of(Integer.MAX_VALUE, Integer.MAX_VALUE)));
    }
}
