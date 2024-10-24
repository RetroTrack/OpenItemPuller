package com.retrotrack.openitempuller.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class VerticalScrollbarWidget extends ClickableWidget {

    private static final Identifier TEXTURE = new Identifier(MOD_ID, "textures/gui/sprites/button/scrollbar/scrollbar.png");

    private final int x;
    private final int y;
    private final int height;
    private final int scrollHeight;
    public double scrollPos;
    private final Callback callback;

    public VerticalScrollbarWidget(int x, int y, int width, int height,int scrollHeight , Text message, Callback callback) {
        super(x, y, width, height, message);
        this.x = x;
        this.y = y;
        this.height = height;
        this.scrollHeight = scrollHeight;
        this.scrollPos = 0;
        this.callback = callback;
    }
    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(TEXTURE, this.x, this.y + (int) ((float) scrollPos / scrollHeight * height), 0, 0, 7, 15, 7, 15);

    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        float position = ((float) (mouseY * scrollHeight) / height) - this.y;
        scrollPos = MathHelper.clamp(position, 0, scrollHeight -14);
        this.callback.onValueChange(scrollPos, scrollHeight -14);
        super.onDrag(mouseX,mouseY,deltaX,deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        scrollPos = MathHelper.clamp(scrollPos - (verticalAmount), 0, scrollHeight -14);
        this.callback.onValueChange(scrollPos, scrollHeight - 14);
        return super.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, "Item list scrollbar");
    }


    @Environment(EnvType.CLIENT)
    public interface Callback {
        IPCheckboxWidget.Callback EMPTY = (checkbox, checked) -> {
        };

        void onValueChange(double scrollbarPos, int scrollHeight);
    }
}

