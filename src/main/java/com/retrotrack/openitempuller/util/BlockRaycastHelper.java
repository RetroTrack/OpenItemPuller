package com.retrotrack.openitempuller.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class BlockRaycastHelper {
    public static BlockPos getBlockPlayerIsLookingAt(ClientPlayerEntity player) {
        // Define the range for the raycast
        double reachDistance = 5.0; // Adjust as needed for the range

        // Start and end positions for the raycast
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(reachDistance));

        // Create a raycast context
        RaycastContext context = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        );

        // Perform the raycast
        BlockHitResult hitResult = player.getWorld().raycast(context);

        // Check if the raycast hit a block
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }

        // Return null if no block was hit
        return null;
    }
}
