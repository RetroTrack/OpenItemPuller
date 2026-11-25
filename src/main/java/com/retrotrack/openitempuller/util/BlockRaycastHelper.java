package com.retrotrack.openitempuller.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlockRaycastHelper {
    public static BlockPos getBlockPlayerIsLookingAt(LocalPlayer player) {
        double reachDistance = 5.0;

        Vec3 start = player.getEyePosition(1.0F);
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(reachDistance));

        ClipContext context = new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);

        BlockHitResult hitResult = player.level().clip(context);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }
        return null;
    }
}
