package com.retrotrack.openitempuller.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only view of where a container screen sits; the only thing the mod still takes from it by mixin. */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int openitempuller$getLeftPos();

    @Accessor("topPos")
    int openitempuller$getTopPos();

    @Accessor("imageWidth")
    int openitempuller$getImageWidth();
}
