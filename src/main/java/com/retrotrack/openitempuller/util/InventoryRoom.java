package com.retrotrack.openitempuller.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Simulates putting items into an inventory, to tell beforehand how much of a pull fits. Free of Minecraft
 * classes so it can be unit tested on its own.
 * <p>
 * Items go where vanilla puts them: first on top of matching, not yet full stacks, then into empty slots.
 * Every {@link #insert} changes the simulated state, so inserting several items in a row correctly competes for
 * the same empty slots.
 *
 * @param <V> what makes two stacks stackable (for items: the item variant, i.e. item plus components)
 */
public final class InventoryRoom<V> {
    /** @param variant what is in the slot, or {@code null} for an empty slot */
    public record Slot<V>(V variant, int count) {}

    private final List<V> variants = new ArrayList<>();
    private final int[] counts;
    private final ToIntFunction<V> maxStack;

    public InventoryRoom(List<Slot<V>> slots, ToIntFunction<V> maxStack) {
        this.maxStack = maxStack;
        this.counts = new int[slots.size()];
        for (int k = 0; k < counts.length; k++) {
            Slot<V> slot = slots.get(k);
            boolean empty = slot.variant() == null || slot.count() <= 0;
            variants.add(empty ? null : slot.variant());
            counts[k] = empty ? 0 : slot.count();
        }
    }

    /**
     * Puts up to {@code wanted} of {@code variant} into the simulated inventory.
     *
     * @return how many actually fit; the simulation now holds them
     */
    public long insert(V variant, long wanted) {
        if (variant == null || wanted <= 0) return 0;
        int max = maxStack.applyAsInt(variant);
        if (max <= 0) return 0;
        long left = wanted;
        for (int k = 0; k < counts.length && left > 0; k++) {
            if (!variant.equals(variants.get(k)) || counts[k] >= max) continue;
            int add = (int) Math.min(left, max - counts[k]);
            counts[k] += add;
            left -= add;
        }
        for (int k = 0; k < counts.length && left > 0; k++) {
            if (variants.get(k) != null) continue;
            int add = (int) Math.min(left, max);
            variants.set(k, variant);
            counts[k] = add;
            left -= add;
        }
        return wanted - left;
    }
}
