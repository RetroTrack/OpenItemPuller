package com.retrotrack.openitempuller.client;

import com.retrotrack.openitempuller.config.ClientConfig;
import com.retrotrack.openitempuller.config.ConfigFiles;
import com.retrotrack.openitempuller.event.KeyInputHandler;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload;
import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ItemPullerClient implements ClientModInitializer {

    /** This player's preferences. Only ever sent to a server as a request, never used as a limit. */
    private static volatile ClientConfig config = ClientConfig.DEFAULT;

    /**
     * Storage the pulled items go to, with its dimension. Lives only for this session (it was never kept
     * across restarts) and is dropped when the connection ends, so it can't leak into another world or server.
     */
    private static volatile GlobalPos targetBlock;

    public static ClientConfig config() {
        return config;
    }

    public static void updateConfig(ClientConfig newConfig) {
        config = newConfig;
        ConfigFiles.saveClient(newConfig);
    }

    /** Sets (or, with {@code null}, clears) the target storage. */
    public static void setTargetBlock(@Nullable GlobalPos target) {
        targetBlock = target;
    }

    /**
     * The target storage if it can be used right now, otherwise {@code null} (items then go to the inventory)
     * and the player is told why. A target that no longer holds a storage is forgotten; one that is merely in
     * another dimension or too far away is kept, so it works again when the player is back.
     *
     * @param serverMaxRadius the server's limit: it checks the target against that, so this does too
     */
    public static @Nullable BlockPos usableTarget(Minecraft client, int serverMaxRadius) {
        GlobalPos target = targetBlock;
        if (target == null || client.level == null || client.player == null) return null;

        String problem = null;
        if (!target.dimension().equals(client.level.dimension())) {
            problem = "openitempuller.message.target_unavailable.other_dimension";
        } else if (!BlockEntitySearchUtil.isValidBlockEntity(target.pos(), client.level)) {
            targetBlock = null;
            problem = "openitempuller.message.target_unavailable.gone";
        } else {
            double max = serverMaxRadius + 2.0; // same slack as the server's range check
            if (client.player.distanceToSqr(Vec3.atCenterOf(target.pos())) > max * max) {
                problem = "openitempuller.message.target_unavailable.out_of_range";
            }
        }
        if (problem == null) return target.pos();
        client.player.sendSystemMessage(Component.translatable(problem).withStyle(ChatFormatting.RED));
        return null;
    }

    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        config = ConfigFiles.loadClient();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> targetBlock = null);
        // Registered here, not in shared code: this entrypoint only ever loads on a client.
        ClientPlayNetworking.registerGlobalReceiver(OpenPullItemScreenPayload.TYPE, ClientPackets::openPullScreen);
        PullButton.register();
        KeyInputHandler.register();
    }
}
