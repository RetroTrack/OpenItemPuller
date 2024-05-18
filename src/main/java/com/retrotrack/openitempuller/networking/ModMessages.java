package com.retrotrack.openitempuller.networking;

import com.retrotrack.openitempuller.networking.packets.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModMessages {

    public static void registerServerPackets() {
        //Client -> Server
        ServerPlayNetworking.registerGlobalReceiver(CheckChestPacket.TYPE, (CheckChestPacket::receiveServer));
        ServerPlayNetworking.registerGlobalReceiver(PullItemsPacket.TYPE, (PullItemsPacket::receiveServer));
        ServerPlayNetworking.registerGlobalReceiver(OpenSettingsScreenPacket.TYPE, (OpenSettingsScreenPacket::receiveServer));
    }

    public static void registerClientPackets() {
        //Server -> Client
        ClientPlayNetworking.registerGlobalReceiver(OpenPullItemScreenPacket.TYPE, (OpenPullItemScreenPacket::receiveClient));
        ClientPlayNetworking.registerGlobalReceiver(OpenSettingsScreenPacket.TYPE, (OpenSettingsScreenPacket::receiveClient));
    }
}
