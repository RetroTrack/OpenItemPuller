package com.retrotrack.itempuller.gui.widget;


import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class IPCheckboxWidget extends PressableWidget {
    private static final Identifier SELECTED_HIGHLIGHTED_TEXTURE = new Identifier("widget/checkbox_selected_highlighted");
    private static final Identifier SELECTED_TEXTURE = new Identifier("widget/checkbox_selected");
    private static final Identifier HIGHLIGHTED_TEXTURE = new Identifier("widget/checkbox_highlighted");
    private static final Identifier TEXTURE = new Identifier("widget/checkbox");
    private static final int TEXT_COLOR = 14737632;
    private static final int field_47105 = 4;
    private static final int field_47106 = 8;
    private boolean checked;
    private final IPCheckboxWidget.Callback callback;

    IPCheckboxWidget(int x, int y, Text message, TextRenderer textRenderer, boolean checked, IPCheckboxWidget.Callback callback) {
        super(x, y, getSize(textRenderer) + 4 + textRenderer.getWidth(message), getSize(textRenderer), message);
        this.checked = checked;
        this.callback = callback;
    }

    public static Builder builder(Text text, TextRenderer textRenderer) {

        return new Builder(text, textRenderer);
    }

    private static int getSize(TextRenderer textRenderer) {
        return 12;
    }

    @Override
    public void onPress() {
        this.checked = !this.checked;
        this.callback.onValueChange(this, this.checked);
    }

    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.focused"));
            } else {
                builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.hovered"));
            }
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        RenderSystem.enableDepthTest();
        TextRenderer textRenderer = minecraftClient.textRenderer;
        context.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        Identifier identifier;
        if (this.checked) {
            identifier = this.isFocused() ? SELECTED_HIGHLIGHTED_TEXTURE : SELECTED_TEXTURE;
        } else {
            identifier = this.isFocused() ? HIGHLIGHTED_TEXTURE : TEXTURE;
        }

        int i = getSize(textRenderer);
        int j = this.getX() + i + 4;
        int k = this.getY() + (this.height >> 1) - (9 >> 1);
        context.drawGuiTexture(identifier, this.getX(), this.getY(), i, i);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawTextWithShadow(textRenderer, this.getMessage(), j, k, 14737632 | MathHelper.ceil(this.alpha * 255.0F) << 24);
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final Text message;
        private final TextRenderer textRenderer;
        private int x = 0;
        private int y = 0;
        private IPCheckboxWidget.Callback callback = IPCheckboxWidget.Callback.EMPTY;
        private boolean checked = false;
        @Nullable
        private SimpleOption<Boolean> option = null;
        @Nullable
        private Tooltip tooltip = null;

        Builder(Text message, TextRenderer textRenderer) {
            this.message = message;
            this.textRenderer = textRenderer;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder callback(IPCheckboxWidget.Callback callback) {
            this.callback = callback;
            return this;
        }

        public Builder checked(boolean checked) {
            this.checked = checked;
            this.option = null;
            return this;
        }

        public Builder option(SimpleOption<Boolean> option) {
            this.option = option;
            this.checked = option.getValue();
            return this;
        }

        public Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public IPCheckboxWidget build() {
            IPCheckboxWidget.Callback callback = this.option == null ? this.callback : (checkbox, checked) -> {
                this.option.setValue(checked);
                this.callback.onValueChange(checkbox, checked);
            };
            IPCheckboxWidget checkboxWidget = new IPCheckboxWidget(this.x, this.y, this.message, this.textRenderer, this.checked, callback);
            checkboxWidget.setTooltip(this.tooltip);
            return checkboxWidget;
        }
    }

    @Environment(EnvType.CLIENT)
    public interface Callback {
        IPCheckboxWidget.Callback EMPTY = (checkbox, checked) -> {
        };

        void onValueChange(IPCheckboxWidget checkbox, boolean checked);
    }
}
