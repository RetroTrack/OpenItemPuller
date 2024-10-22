package com.retrotrack.openitempuller.mixin;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

// Mixin class to add pull and settings buttons to various screen types
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    // Shadow Variables
    @Final
    @Shadow protected ScreenHandler handler;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;

    // Unique variables
    @Unique private TexturedButtonWidget pullButton;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    // Injecting code to render the buttons
    @Inject(method = "render", at = @At("HEAD"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Skip rendering if the screen is a creative inventory screen
        if (this.handler instanceof CreativeInventoryScreen.CreativeScreenHandler) return;

        // Always reposition the button, even after another screen is closed or resized
        repositionButton();
    }

    // Injecting code to initialize the buttons
    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo info) {
        // Skip initialization if the screen is a creative inventory screen
        if (this.handler instanceof CreativeInventoryScreen.CreativeScreenHandler) return;

        // Render pull button
        renderPullButton();
    }

    // Method to render the pull button
    @Unique
    private void renderPullButton() {
        // Return if client instance is null
        if (this.client == null) return;

        // Create pull button and attach action for checking chests
        pullButton = this.addDrawableChild(new TexturedButtonWidget(
                // The initial position is set, but we'll update it in `repositionButton`
                this.x + this.backgroundWidth - 21,
                this.y - 17,
                20, 18,
                new ButtonTextures(
                        Identifier.of(MOD_ID, "button/pull/pull_button" /* default */),
                        Identifier.of(MOD_ID, "button/pull/pull_button_highlighted" /* highlighted */)
                ),
                (button) -> ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius")))
        ));

        // Reposition the button
        repositionButton();
    }

    // Method to reposition the button to the top-right corner
    @Unique
    private void repositionButton() {
        if (this.pullButton != null) {
            // Calculate the new position, ensuring the button is always in the top-right corner
            int buttonX = this.x + this.backgroundWidth - 21;
            int buttonY = this.y - 17;

            pullButton.setPosition(buttonX, buttonY);
        }
    }
}

