package com.retrotrack.openitempuller.client;

import com.retrotrack.openitempuller.mixin.AbstractContainerScreenAccessor;
import com.retrotrack.openitempuller.networking.ClientNetworkUtil;
import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

/**
 * Adds the "pull" button to the top-right corner of container screens, using Fabric's screen events
 * instead of a mixin into {@code AbstractContainerScreen}. Only the screen's position is read through an
 * accessor mixin ({@link AbstractContainerScreenAccessor}).
 */
@Environment(EnvType.CLIENT)
public final class PullButton {
    private static final int WIDTH = 20;
    private static final int HEIGHT = 18;

    private PullButton() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
            if (containerScreen instanceof CreativeModeInventoryScreen) return;
            AbstractContainerScreenAccessor position = (AbstractContainerScreenAccessor) containerScreen;

            ImageButton button = new ImageButton(
                    x(position), y(position), WIDTH, HEIGHT,
                    new WidgetSprites(
                            Identifier.fromNamespaceAndPath(MOD_ID, "button/pull/pull_button"),
                            Identifier.fromNamespaceAndPath(MOD_ID, "button/pull/pull_button_highlighted")),
                    b -> ClientNetworkUtil.trySend(new CheckChestPayload(ItemPullerClient.config().radius())),
                    CommonComponents.EMPTY);
            Screens.getWidgets(screen).add(button);

            // The screen moves when e.g. the recipe book opens: keep the button in the corner.
            // Events registered for one screen are dropped when it is re-initialised, then this runs again.
            ScreenEvents.afterTick(screen).register(s -> button.setPosition(x(position), y(position)));
        });
    }

    private static int x(AbstractContainerScreenAccessor position) {
        return position.openitempuller$getLeftPos() + position.openitempuller$getImageWidth() - 21;
    }

    private static int y(AbstractContainerScreenAccessor position) {
        return position.openitempuller$getTopPos() - 17;
    }
}
