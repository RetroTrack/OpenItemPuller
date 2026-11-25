package com.retrotrack.openitempuller.mixin;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

// Adds the "pull" button to container screens
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    @Final @Shadow protected AbstractContainerMenu menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Final @Shadow protected int imageWidth;

    @Unique private ImageButton openitempuller$pullButton;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    // Keep the button in the top-right corner, also after the screen moves (e.g. recipe book)
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void openitempuller$reposition(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.menu instanceof CreativeModeInventoryScreen.ItemPickerMenu) return;
        openitempuller$repositionButton();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void openitempuller$init(CallbackInfo info) {
        if (this.menu instanceof CreativeModeInventoryScreen.ItemPickerMenu) return;
        if (this.minecraft == null) return;

        openitempuller$pullButton = this.addRenderableWidget(new ImageButton(
                this.leftPos + this.imageWidth - 21,
                this.topPos - 17,
                20, 18,
                new WidgetSprites(
                        Identifier.fromNamespaceAndPath(MOD_ID, "button/pull/pull_button"),
                        Identifier.fromNamespaceAndPath(MOD_ID, "button/pull/pull_button_highlighted")),
                button -> ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius"))),
                CommonComponents.EMPTY));
    }

    @Unique
    private void openitempuller$repositionButton() {
        if (this.openitempuller$pullButton != null) {
            openitempuller$pullButton.setPosition(this.leftPos + this.imageWidth - 21, this.topPos - 17);
        }
    }
}
