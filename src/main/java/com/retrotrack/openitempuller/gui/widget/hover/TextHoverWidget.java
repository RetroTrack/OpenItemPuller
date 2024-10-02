package com.retrotrack.openitempuller.gui.widget.hover;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class TextHoverWidget extends TextHoverButtonWidget {
    protected final ButtonTextures textures;

    public TextHoverWidget(int x, int y, int width, int height, ButtonTextures textures, TextHoverButtonWidget.PressAction pressAction) {
        this(x, y, width, height, textures, pressAction, ScreenTexts.EMPTY);
    }

    public TextHoverWidget(int x, int y, int width, int height, ButtonTextures textures, TextHoverButtonWidget.PressAction pressAction, Text text) {
        super(x, y, width, height, text, pressAction, DEFAULT_NARRATION_SUPPLIER);
        this.textures = textures;
    }
    public TextHoverWidget(int x, int y, int width, int height) {
        super(x, y, width, height, ScreenTexts.EMPTY, a -> {}, DEFAULT_NARRATION_SUPPLIER);
        this.textures = new ButtonTextures(Identifier.of(MOD_ID, "button/invisible/invisible"),
                Identifier.of(MOD_ID, "button/invisible/invisible"));
    }

    public TextHoverWidget(int width, int height, ButtonTextures textures, TextHoverButtonWidget.PressAction pressAction, Text text) {
        this(0, 0, width, height, textures, pressAction, text);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier identifier = this.textures.get(this.isNarratable(), this.isSelected());
        context.drawGuiTexture(identifier, this.getX(), this.getY(), this.width, this.height);
    }
}
