package com.retrotrack.openitempuller.gui.widget.hover;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class TextHoverButtonWidget extends NonPressableWidget {
    protected static final TextHoverButtonWidget.NarrationSupplier DEFAULT_NARRATION_SUPPLIER = textSupplier -> (MutableText)textSupplier.get();
    protected final TextHoverButtonWidget.PressAction onPress;
    protected final TextHoverButtonWidget.NarrationSupplier narrationSupplier;

    public static TextHoverButtonWidget.Builder builder(Text message, TextHoverButtonWidget.PressAction onPress) {
        return new TextHoverButtonWidget.Builder(message, onPress);
    }

    protected TextHoverButtonWidget(int x, int y, int width, int height, Text message,
                                    TextHoverButtonWidget.PressAction onPress, TextHoverButtonWidget.NarrationSupplier narrationSupplier) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.narrationSupplier = narrationSupplier;
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected MutableText getNarrationMessage() {
        return this.narrationSupplier.createNarrationMessage(super::getNarrationMessage);
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final Text message;
        private final TextHoverButtonWidget.PressAction onPress;
        @Nullable
        private Tooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private TextHoverButtonWidget.NarrationSupplier narrationSupplier = TextHoverButtonWidget.DEFAULT_NARRATION_SUPPLIER;

        public Builder(Text message, TextHoverButtonWidget.PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public TextHoverButtonWidget.Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public TextHoverButtonWidget.Builder width(int width) {
            this.width = width;
            return this;
        }

        public TextHoverButtonWidget.Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public TextHoverButtonWidget.Builder dimensions(int x, int y, int width, int height) {
            return this.position(x, y).size(width, height);
        }

        public TextHoverButtonWidget.Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public TextHoverButtonWidget.Builder narrationSupplier(TextHoverButtonWidget.NarrationSupplier narrationSupplier) {
            this.narrationSupplier = narrationSupplier;
            return this;
        }

        public TextHoverButtonWidget build() {
            TextHoverButtonWidget textHoverButtonWidget = new TextHoverButtonWidget(this.x, this.y,
                    this.width, this.height, this.message, this.onPress, this.narrationSupplier);
            textHoverButtonWidget.setTooltip(this.tooltip);
            return textHoverButtonWidget;
        }
    }

    @Environment(EnvType.CLIENT)
    public interface NarrationSupplier {
        MutableText createNarrationMessage(Supplier<MutableText> textSupplier);
    }

    @Environment(EnvType.CLIENT)
    public interface PressAction {
        void onPress(TextHoverButtonWidget button);
    }
}
