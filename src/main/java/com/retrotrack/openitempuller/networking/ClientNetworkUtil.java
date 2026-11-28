package com.retrotrack.openitempuller.networking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client-side helper for sending payloads to servers that may not have this mod installed. */
@Environment(EnvType.CLIENT)
public final class ClientNetworkUtil {
    private ClientNetworkUtil() {}

    /**
     * Sends the payload only if the server has registered a receiver for it.
     * Otherwise shows a friendly message instead of sending (which would throw / disconnect).
     *
     * @return true if the payload was sent
     */
    public static boolean trySend(CustomPacketPayload payload) {
        if (!ClientPlayNetworking.canSend(payload.type())) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.translatable("openitempuller.message.server_missing_mod").withStyle(ChatFormatting.RED));
            }
            return false;
        }
        ClientPlayNetworking.send(payload);
        return true;
    }
}
