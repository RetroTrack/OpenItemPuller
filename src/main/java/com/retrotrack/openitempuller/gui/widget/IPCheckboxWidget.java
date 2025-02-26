package com.retrotrack.openitempuller.gui.widget;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class IPCheckboxWidget extends PressableWidget {
    private static final Identifier SELECTED_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/checkbox_selected_highlighted");
    private static final Identifier SELECTED_TEXTURE = Identifier.ofVanilla("widget/checkbox_selected");
    private static final Identifier HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/checkbox_highlighted");
    private static final Identifier TEXTURE = Identifier.ofVanilla("widget/checkbox");
    private boolean checked;
    private final IPCheckboxWidget.Callback callback;
    private final MultilineTextWidget textWidget;

    IPCheckboxWidget(int x, int y, int maxWidth, Text message, TextRenderer textRenderer, boolean checked, IPCheckboxWidget.Callback callback) {
        super(x, y, 0, 0, message);
        this.width = this.calculateWidth(maxWidth, message, textRenderer);
        this.textWidget = (new MultilineTextWidget(message, textRenderer)).setMaxWidth(this.width).setTextColor(14737632);
        this.height = this.calculateHeight(textRenderer);
        this.checked = checked;
        this.callback = callback;
    }

    private int calculateWidth(int max, Text text, TextRenderer textRenderer) {
        return Math.min(calculateWidth(text, textRenderer), max);
    }

    private int calculateHeight(TextRenderer textRenderer) {
        return Math.max(getCheckboxSize(textRenderer), this.textWidget.getHeight());
    }

    static int calculateWidth(Text text, TextRenderer textRenderer) {
        return getCheckboxSize(textRenderer) + 4 + textRenderer.getWidth(text);
    }

    public static IPCheckboxWidget.Builder builder(Text text, TextRenderer textRenderer) {
        return new IPCheckboxWidget.Builder(text, textRenderer);
    }

    public static int getCheckboxSize(TextRenderer textRenderer) {
        Objects.requireNonNull(textRenderer);
        return 12;
    }

    public void onPress() {
        this.checked = !this.checked;
        this.callback.onValueChange(this, this.checked);
    }

    public boolean isChecked() {
        return this.checked;
    }

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

    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        TextRenderer textRenderer = minecraftClient.textRenderer;
        Identifier identifier;
        if (this.checked) {
            identifier = this.isFocused() ? SELECTED_HIGHLIGHTED_TEXTURE : SELECTED_TEXTURE;
        } else {
            identifier = this.isFocused() ? HIGHLIGHTED_TEXTURE : TEXTURE;
        }

        int i = getCheckboxSize(textRenderer);
        context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, this.getX(), this.getY(), i, i, ColorHelper.getWhite(this.alpha));
        int j = this.getX() + i + 4;
        int k = this.getY() + i / 2 - this.textWidget.getHeight() / 2;
        this.textWidget.setPosition(j, k);
        this.textWidget.renderWidget(context, mouseX, mouseY, delta);
    }

    @Environment(EnvType.CLIENT)
    public interface Callback {
        IPCheckboxWidget.Callback EMPTY = (checkbox, checked) -> {
        };

        void onValueChange(IPCheckboxWidget checkbox, boolean checked);
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final Text message;
        private final TextRenderer textRenderer;
        private int maxWidth;
        private int x = 0;
        private int y = 0;
        private IPCheckboxWidget.Callback callback;
        private boolean checked;
        @Nullable
        private SimpleOption<Boolean> option;
        @Nullable
        private Tooltip tooltip;

        Builder(Text message, TextRenderer textRenderer) {
            this.callback = IPCheckboxWidget.Callback.EMPTY;
            this.checked = false;
            this.option = null;
            this.tooltip = null;
            this.message = message;
            this.textRenderer = textRenderer;
            this.maxWidth = IPCheckboxWidget.calculateWidth(message, textRenderer);
        }

        public IPCheckboxWidget.Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public IPCheckboxWidget.Builder callback(IPCheckboxWidget.Callback callback) {
            this.callback = callback;
            return this;
        }

        public IPCheckboxWidget.Builder checked(boolean checked) {
            this.checked = checked;
            this.option = null;
            return this;
        }

        public IPCheckboxWidget.Builder option(SimpleOption<Boolean> option) {
            this.option = option;
            this.checked = option.getValue();
            return this;
        }

        public IPCheckboxWidget.Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public IPCheckboxWidget.Builder maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public IPCheckboxWidget build() {
            IPCheckboxWidget.Callback callback = this.option == null ? this.callback : (checkbox, checked) -> {
                this.option.setValue(checked);
                this.callback.onValueChange(checkbox, checked);
            };
            IPCheckboxWidget checkboxWidget = new IPCheckboxWidget(this.x, this.y, this.maxWidth, this.message, this.textRenderer, this.checked, callback);
            checkboxWidget.setTooltip(this.tooltip);
            return checkboxWidget;
        }
    }
}
