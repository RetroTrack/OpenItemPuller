package com.retrotrack.openitempuller.util;

import java.util.List;

/**
 * The maths behind the quick amount buttons ("1", "Stack", "All"), kept free of Minecraft classes so it can be
 * unit tested on its own.
 */
public final class QuickAmount {
    private QuickAmount() {}

    /**
     * Spreads {@code wanted} items over the containers in the order given: the first container is emptied as far
     * as needed, then the next, and so on. That is the order the list on screen shows, so what you see at the top
     * is what gets taken first.
     *
     * @param available how many of the item each container holds (negative values count as 0)
     * @param wanted    the total to take; zero or less gives all zeros, more than exists takes everything
     * @return the amount to take per container, same length and order as {@code available}
     */
    public static int[] distribute(List<Integer> available, long wanted) {
        int[] amounts = new int[available.size()];
        long left = wanted;
        for (int k = 0; k < amounts.length && left > 0; k++) {
            int have = Math.max(0, available.get(k));
            int take = (int) Math.min(have, left);
            amounts[k] = take;
            left -= take;
        }
        return amounts;
    }

    /** Takes everything: every container gives up all of its items. */
    public static int[] all(List<Integer> available) {
        return distribute(available, Long.MAX_VALUE);
    }
}
