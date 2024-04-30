package com.retrotrack.openitempuller;

import com.retrotrack.openitempuller.config.ItemPullerConfig;
import net.fabricmc.api.ModInitializer;

import static com.retrotrack.openitempuller.config.ItemPullerConfig.initConfig;
import static com.retrotrack.openitempuller.networking.ModMessages.registerC2SPackets;

public class ItemPuller implements ModInitializer {

    public static ItemPullerConfig.Config CONFIG;
    public static final String MOD_ID = "open_item_puller";
    /**
     * Runs the mod initializer. (both client and server)
     */
    @Override
    public void onInitialize() {
        initConfig();
        registerC2SPackets();
    }
}
