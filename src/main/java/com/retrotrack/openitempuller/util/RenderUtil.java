package com.retrotrack.openitempuller.util;

import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.screen.PlayerScreenHandler;
import org.joml.Matrix4fStack;

public class RenderUtil {

    private static void renderItemAt(ItemStack item, int x, int y, float scaleFactor, ItemRenderer itemRenderer) {
        // Set texture and blending
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // Transformation matrix for rendering
        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.translate(x, y, 100.0F);
        matrixStack.scale(1.0F, -1.0F, 1.0F);
        matrixStack.scale(16.0F, 16.0F, 16.0F);
        RenderSystem.applyModelViewMatrix();

        // Scaling matrix
        MatrixStack matrixStack2 = new MatrixStack();
        matrixStack2.scale(scaleFactor, scaleFactor, scaleFactor);

        // Rendering setup
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        BakedModel model = itemRenderer.getModel(item, null, null, 0);
        boolean disableGuiDepthLighting = !model.isSideLit();

        // Disable GUI depth lighting if not side-lit
        if (disableGuiDepthLighting) {
            DiffuseLighting.disableGuiDepthLighting();
        }

        // Render the ItemStack
        itemRenderer.renderItem(item, ModelTransformationMode.GUI, false, matrixStack2, immediate, 15728880, OverlayTexture.DEFAULT_UV, model);
        immediate.draw();
        RenderSystem.enableDepthTest();

        // Enable GUI depth lighting if previously disabled
        if (disableGuiDepthLighting) {
            DiffuseLighting.enableGuiDepthLighting();
        }

        // Pop the matrix stack
        matrixStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    public static void renderItemAt(RenderVariables renderVariables) {
        renderItemAt(renderVariables.itemStack, renderVariables.x, renderVariables.y, renderVariables.scaleFactor, renderVariables.itemRenderer);
    }

    public record RenderVariables(ItemStack itemStack, int x, int y, float scaleFactor, ItemRenderer itemRenderer) {}
}
