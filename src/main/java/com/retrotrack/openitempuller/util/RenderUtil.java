package com.retrotrack.openitempuller.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.OverlayTexture;

@Environment(EnvType.CLIENT)
public class RenderUtil {

    private static void renderItemAt(ItemStack item, int x, int y, float scaleFactor) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemRenderer itemRenderer = client.getItemRenderer();

        // Prepare the matrix stack
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(x, y, 100.0F);
        matrices.scale(scaleFactor * 16.0F, scaleFactor * -16.0F, scaleFactor * 16.0F);

        // Get the vertex consumer provider from the client
        VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();

        // Render the item in GUI context
        itemRenderer.renderItem(item, ItemDisplayContext.GUI, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, null,0);

        // Draw the vertex consumers buffer
        vertexConsumers.draw();

        matrices.pop();
    }

    public static void renderItemAt(RenderVariables renderVariables) {
        renderItemAt(renderVariables.itemStack, renderVariables.x, renderVariables.y, renderVariables.scaleFactor);
    }

    public record RenderVariables(ItemStack itemStack, int x, int y, float scaleFactor) {}
}
