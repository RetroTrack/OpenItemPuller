package com.retrotrack.openitempuller.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class RenderUtil {

    /** Draws an item centered on (x, y), scaled by scaleFactor (a normal item is 16x16). */
    public static void renderItemAt(GuiGraphicsExtractor graphics, RenderVariables v) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(v.x() - 8f * v.scaleFactor(), v.y() - 8f * v.scaleFactor());
        pose.scale(v.scaleFactor(), v.scaleFactor());
        graphics.item(v.itemStack(), 0, 0);
        pose.popMatrix();
    }

    public record RenderVariables(ItemStack itemStack, int x, int y, float scaleFactor) {}
}
