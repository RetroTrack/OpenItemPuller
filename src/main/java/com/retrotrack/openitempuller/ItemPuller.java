package com.retrotrack.openitempuller;

import com.retrotrack.openitempuller.config.ConfigFiles;
import com.retrotrack.openitempuller.config.ServerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import static com.retrotrack.openitempuller.networking.ModNetworking.registerCommonPackets;
import static com.retrotrack.openitempuller.networking.ModNetworking.registerServerPackets;

public class ItemPuller implements ModInitializer {

    public static final String MOD_ID = "openitempuller";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

    /** Server rules (dedicated server, or the integrated server in singleplayer). Never holds player preferences. */
    private static volatile ServerConfig serverConfig = ServerConfig.DEFAULT;

    public static ServerConfig serverConfig() {
        return serverConfig;
    }

    /**
     * Runs the mod initializer. (both client and server)
     */
    @Override
    public void onInitialize() {
        ConfigFiles.migrateLegacy(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
        serverConfig = ConfigFiles.loadServer();
        registerCommonPackets();
        registerServerPackets();
    }

    public static int getInt(String str) {
        try {return Integer.parseInt(str);}
        catch (NumberFormatException e) {return -1;}
    }
}
