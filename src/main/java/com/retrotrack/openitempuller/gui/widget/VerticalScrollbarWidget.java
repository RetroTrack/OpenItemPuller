package com.retrotrack.openitempuller.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class VerticalScrollbarWidget extends AbstractWidget {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/sprites/button/scrollbar/scrollbar.png");

    private final int scrollHeight;
    public double scrollPos;
    private final Callback callback;

    public VerticalScrollbarWidget(int x, int y, int width, int height, int scrollHeight, Component message, Callback callback) {
        super(x, y, width, height, message);
        this.scrollHeight = scrollHeight;
        this.scrollPos = 0;
        this.callback = callback;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(),
                this.getY() + (int) ((float) scrollPos / scrollHeight * this.getHeight()),
                0f, 0f, 7, 15, 7, 15);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        // Mouse offset inside the track, scaled to scroll units (the thumb is drawn at getY() + scrollPos / scrollHeight * height)
        double position = (event.y() - this.getY()) * scrollHeight / this.getHeight();
        scrollPos = Mth.clamp(position, 0, scrollHeight - 14);
        this.callback.onValueChange(scrollPos, scrollHeight - 14);
        return true;
    }

    /** Called by the screen so the wheel works anywhere on it. */
    public void scrollBy(double verticalAmount) {
        scrollPos = Mth.clamp(scrollPos - verticalAmount, 0, scrollHeight - 14);
        this.callback.onValueChange(scrollPos, scrollHeight - 14);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Environment(EnvType.CLIENT)
    public interface Callback {
        void onValueChange(double scrollbarPos, int scrollHeight);
    }
}
