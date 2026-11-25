package com.retrotrack.openitempuller.gui.widget.hover;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;

/**
 * Invisible rectangle that only exists to show a tooltip while hovered.
 * Replaces the five copied hover widget classes of the 1.21.4 version.
 */
@Environment(EnvType.CLIENT)
public class TextHoverWidget extends AbstractWidget {

    public TextHoverWidget(int x, int y, int width, int height) {
        super(x, y, width, height, CommonComponents.EMPTY);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // nothing to draw; AbstractWidget draws the tooltip itself while hovered
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }
}
