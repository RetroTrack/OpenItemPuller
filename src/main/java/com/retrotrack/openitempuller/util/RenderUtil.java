package com.retrotrack.openitempuller.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class RenderUtil {


    private static void renderItemAt(ItemStack item, int x, int y, float scaleFactor, ItemRenderer itemRenderer) {
        MatrixStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.push();
        modelViewStack.translate(x, y, 100.0F);
        modelViewStack.scale(1.0F, -1.0F, 1.0F);
        modelViewStack.scale(16.0F, 16.0F, 16.0F);
        RenderSystem.applyModelViewMatrix();

        MatrixStack renderStack = new MatrixStack();
        renderStack.scale(scaleFactor, scaleFactor, scaleFactor);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        BakedModel model = itemRenderer.getModel(item, null, null, 0);
        boolean disableGuiDepthLighting = !model.isSideLit();

        if (disableGuiDepthLighting) DiffuseLighting.disableGuiDepthLighting();

        itemRenderer.renderItem(item, ModelTransformationMode.GUI, false, renderStack, immediate, 15728880, OverlayTexture.DEFAULT_UV, model);
        immediate.draw();
        RenderSystem.enableDepthTest();

        if (disableGuiDepthLighting) DiffuseLighting.enableGuiDepthLighting();

        modelViewStack.pop();
        RenderSystem.applyModelViewMatrix();
    }


    public static void renderItemAt(RenderVariables renderVariables) {
        renderItemAt(renderVariables.itemStack, renderVariables.x, renderVariables.y, renderVariables.scaleFactor, renderVariables.itemRenderer);
    }

    public record RenderVariables(ItemStack itemStack, int x, int y, float scaleFactor, ItemRenderer itemRenderer) {}
}
