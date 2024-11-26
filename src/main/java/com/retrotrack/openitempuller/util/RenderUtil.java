package com.retrotrack.openitempuller.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

public class RenderUtil {


    private static void renderItemAt(ItemStack item, int x, int y, float scaleFactor, ItemRenderer itemRenderer) {
        BakedModel model = itemRenderer.getModel(item, null, null, 0);

        MatrixStack contextMatrices = new MatrixStack();
        contextMatrices.push();
        contextMatrices.translate(x, y, 100.0F);
        contextMatrices.scale(16.0F, -16.0F, 16.0F);
        contextMatrices.scale(scaleFactor, scaleFactor, scaleFactor);

        VertexConsumerProvider.Immediate immediateVertexConsumer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        boolean disableGuiDepthLighting = !model.isSideLit();
        if (disableGuiDepthLighting) {
            DiffuseLighting.disableGuiDepthLighting();
        }

        itemRenderer.renderItem(item, ModelTransformationMode.GUI, false, contextMatrices, immediateVertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, model);
        immediateVertexConsumer.draw();
        if (disableGuiDepthLighting) {
            DiffuseLighting.enableGuiDepthLighting();
        }

        contextMatrices.pop();
    }

    public static void renderItemAt(RenderVariables renderVariables) {
        renderItemAt(renderVariables.itemStack, renderVariables.x, renderVariables.y, renderVariables.scaleFactor, renderVariables.itemRenderer);
    }

    public record RenderVariables(ItemStack itemStack, int x, int y, float scaleFactor, ItemRenderer itemRenderer) {}
}
