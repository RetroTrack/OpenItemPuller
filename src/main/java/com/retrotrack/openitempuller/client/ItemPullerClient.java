package com.retrotrack.openitempuller.client;

import net.fabricmc.api.ClientModInitializer;

import static com.retrotrack.openitempuller.networking.ModNetworking.registerClientPackets;

public class ItemPullerClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        registerClientPackets();
    }
}
