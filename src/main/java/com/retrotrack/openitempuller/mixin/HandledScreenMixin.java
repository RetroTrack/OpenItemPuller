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
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
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
    // Fields shadowed from the parent class
    @Shadow protected int backgroundHeight;
    @Final
    @Shadow protected ScreenHandler handler;
    @Shadow protected int x;

    // Unique fields for this mixin
    @Unique private TexturedButtonWidget pullButton;
    @Unique private int lastX;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    // Injecting code to render the buttons
    @Inject(method = "render", at = @At("TAIL"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Skip rendering if the screen is a creative inventory screen
        if (this.handler instanceof CreativeInventoryScreen.CreativeScreenHandler) return;
        // Adjust button positions if the screen has been scrolled horizontally
        if (lastX != x) {
            lastX = x;
            pullButton.setPosition(this.x + 154, (this.height / 2 - this.backgroundHeight / 2
                    - (this.handler instanceof ShulkerBoxScreenHandler || this.handler instanceof HopperScreenHandler ? 18 : 17)));
        }
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
        pullButton = this.addDrawableChild(new TexturedButtonWidget(this.x + 154,
                (this.height / 2 - this.backgroundHeight / 2 - (this.handler instanceof ShulkerBoxScreenHandler || this.handler instanceof HopperScreenHandler ? 18 : 17)),
                20, 18,
                new ButtonTextures(Identifier.of(MOD_ID, "button/pull/pull_button"/*default*/), Identifier.of(MOD_ID, "button/pull/pull_button_highlighted"/*highlighted*/)),
                (button) -> ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius")))));
    }
}
