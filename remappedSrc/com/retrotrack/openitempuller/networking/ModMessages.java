package com.retrotrack.openitempuller.networking;

import com.retrotrack.openitempuller.networking.packets.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public class ModMessages {
    public static final Identifier CHECK_CHESTS = new Identifier(MOD_ID, "check_chests");
    public static final Identifier PULL_ITEMS = new Identifier(MOD_ID, "pull_items");
    public static final Identifier OPEN_PULL_ITEM_SCREEN = new Identifier(MOD_ID, "open_pull_item_screen");

    public static final Identifier OPEN_SETTINGS_SCREEN_C2S = new Identifier(MOD_ID, "open_settings_screen_c2s");
    public static final Identifier OPEN_SETTINGS_SCREEN_S2C = new Identifier(MOD_ID, "open_settings_screen_s2c");

    public static void registerC2SPackets() {
        //Client -> Server
        ServerPlayNetworking.registerGlobalReceiver(CHECK_CHESTS, CheckChestsC2SPacket::receive);
        ServerPlayNetworking.registerGlobalReceiver(PULL_ITEMS, PullItemsC2SPacket::receive);
        ServerPlayNetworking.registerGlobalReceiver(OPEN_SETTINGS_SCREEN_C2S, OpenSettingsScreenC2SPacket::receive);
    }

    public static void registerS2CPackets() {
        //Server -> Client
        ClientPlayNetworking.registerGlobalReceiver(OPEN_PULL_ITEM_SCREEN, OpenPullItemScreenS2CPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(OPEN_SETTINGS_SCREEN_S2C, OpenSettingsScreenS2CPacket::receive);
    }
}
