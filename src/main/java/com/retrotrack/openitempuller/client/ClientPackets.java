package com.retrotrack.openitempuller.client;

import com.retrotrack.openitempuller.gui.screen.PullItemScreen;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload;
import com.retrotrack.openitempuller.util.decoding.NbtCoder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Receivers for clientbound payloads. Lives in the client package on purpose: the payload records themselves
 * are loaded on a dedicated server too and must not reference any client class.
 */
@Environment(EnvType.CLIENT)
public final class ClientPackets {
    private ClientPackets() {}

    public static void openPullScreen(OpenPullItemScreenPayload payload, ClientPlayNetworking.Context context) {
        // A second response (e.g. double press) must not stack another screen on top of the first.
        if (context.client().gui.screen() instanceof PullItemScreen) return;
        context.client().gui.setScreen(new PullItemScreen(context.client().gui.screen(),
                NbtCoder.decode(payload.variants(), payload.chests()), payload.serverMaxRadius()));
    }
}
