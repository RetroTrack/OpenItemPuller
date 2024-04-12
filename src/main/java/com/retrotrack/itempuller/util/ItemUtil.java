package com.retrotrack.itempuller.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class ItemUtil {
    private static void addStackWithMerge(List<ItemStack> stacks, ItemStack newStack) {
        if (newStack.getItem() == Items.AIR) return;
        if (newStack.isStackable() && newStack.getCount() != newStack.getMaxCount())
            for (int j = stacks.size() - 1; j >= 0; j--) {
                ItemStack oldStack = stacks.get(j);
                if (canMergeItems(newStack, oldStack)) {
                    combineStacks(newStack, oldStack);
                    if (oldStack.getItem() == Items.AIR || oldStack.getCount() == 0) stacks.remove(j);
                }
            }
        stacks.add(newStack);
    }

    private static void combineStacks(ItemStack stack, ItemStack stack2) {
        if (stack.getMaxCount() >= stack.getCount() + stack2.getCount()) {
            stack.increment(stack2.getCount());
            stack2.setCount(0);
        }
        int maxInsertAmount = Math.min(stack.getMaxCount() - stack.getCount(), stack2.getCount());
        stack.increment(maxInsertAmount);
        stack2.decrement(maxInsertAmount);
    }

    private static boolean canMergeItems(ItemStack itemStack_1, ItemStack itemStack_2) {
        if (!itemStack_1.isStackable() || !itemStack_2.isStackable())
            return false;
        if (itemStack_1.getCount() == itemStack_1.getMaxCount() || itemStack_2.getCount() == itemStack_2.getMaxCount())
            return false;
        if (itemStack_1.getItem() != itemStack_2.getItem())
            return false;
        if (itemStack_1.getDamage() != itemStack_2.getDamage())
            return false;
        return ItemStack.canCombine(itemStack_1, itemStack_2);
    }
}
