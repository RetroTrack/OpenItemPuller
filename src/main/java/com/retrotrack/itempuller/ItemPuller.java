package com.retrotrack.itempuller;

import net.fabricmc.api.ModInitializer;

import static com.retrotrack.itempuller.networking.ModMessages.registerC2SPackets;

public class ItemPuller implements ModInitializer {

    public static final String MOD_ID = "itempuller";
    /**
     * Runs the mod initializer. (both client and server)
     */
    @Override
    public void onInitialize() {
        registerC2SPackets();
    }
}
