package com.retrotrack.openitempuller.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class RenderUtil {

    private static void renderItemAt(ItemStack item, int x, int y, float scaleFactor) {
        MinecraftClient client = MinecraftClient.getInstance();

        ItemRenderState renderState = new ItemRenderState();
        client.getItemModelManager().update(renderState, item, ModelTransformationMode.GUI, false,null, null, 0);
        VertexConsumerProvider.Immediate immediateVertexConsumer = client.getBufferBuilders().getEntityVertexConsumers();

        boolean disableGuiDepthLighting = !renderState.isSideLit();
        if (disableGuiDepthLighting) {
            immediateVertexConsumer.draw();
            DiffuseLighting.disableGuiDepthLighting();
        }

        MatrixStack contextMatrices = new MatrixStack();
        contextMatrices.push();
        contextMatrices.translate(x, y, 100.0F);
        contextMatrices.scale(16.0F, -16.0F, 16.0F);
        contextMatrices.scale(scaleFactor, scaleFactor, scaleFactor);

        renderState.render(contextMatrices, immediateVertexConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        immediateVertexConsumer.draw();

        contextMatrices.pop();
        if (disableGuiDepthLighting) {
            DiffuseLighting.enableGuiDepthLighting();
        }

    }

    public static void renderItemAt(RenderVariables renderVariables) {
        renderItemAt(renderVariables.itemStack, renderVariables.x, renderVariables.y, renderVariables.scaleFactor);
    }

    public record RenderVariables(ItemStack itemStack, int x, int y, float scaleFactor) {}
}
