package com.retrotrack.itempuller.client;

import net.fabricmc.api.ClientModInitializer;

import static com.retrotrack.itempuller.networking.ModMessages.registerS2CPackets;

public class ItemPullerClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        registerS2CPackets();
    }
}
