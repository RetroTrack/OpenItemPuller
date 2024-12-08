package com.retrotrack.openitempuller;

import com.retrotrack.openitempuller.config.ItemPullerConfig;
import net.fabricmc.api.ModInitializer;

import static com.retrotrack.openitempuller.config.ItemPullerConfig.*;
import static com.retrotrack.openitempuller.networking.ModNetworking.registerServerPackets;

public class ItemPuller implements ModInitializer {

    public static ItemPullerConfig.Config CONFIG;
    public static final String MOD_ID = "openitempuller";
    /**
     * Runs the mod initializer. (both client and server)
     */
    @Override
    public void onInitialize() {
        initConfig();
        registerServerPackets();
        CONFIG.putProperty("is_target_block", false);
        saveConfig(CONFIG, CONFIG_FILE);
    }

    public static int getInt(String str) {
        try {return Integer.parseInt(str);}
        catch (NumberFormatException e) {return -1;}
    }
}
