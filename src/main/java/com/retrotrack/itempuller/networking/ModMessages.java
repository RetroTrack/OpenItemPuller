package com.retrotrack.itempuller.networking;

import com.retrotrack.itempuller.networking.packets.CheckChestsC2SPacket;
import com.retrotrack.itempuller.networking.packets.OpenPullItemScreenS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

import static com.retrotrack.itempuller.ItemPuller.MOD_ID;

public class ModMessages {
    public static final Identifier CHECK_CHESTS = new Identifier(MOD_ID, "check_chests");
    public static final Identifier OPEN_PULL_ITEM_SCREEN = new Identifier(MOD_ID, "open_pull_item_screen");


    public static void registerC2SPackets() {
        //Client -> Server
        ServerPlayNetworking.registerGlobalReceiver(CHECK_CHESTS, CheckChestsC2SPacket::receive);
    }

    public static void registerS2CPackets() {
        //Server -> Client
        ClientPlayNetworking.registerGlobalReceiver(OPEN_PULL_ITEM_SCREEN, OpenPullItemScreenS2CPacket::receive);
    }
}
