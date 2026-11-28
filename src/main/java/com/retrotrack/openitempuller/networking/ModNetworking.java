package com.retrotrack.openitempuller.networking;

import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload;
import com.retrotrack.openitempuller.networking.payloads.PullItemsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModNetworking {

    public static void registerCommonPackets() {
        //Client -> Server
        PayloadTypeRegistry.serverboundPlay().register(CheckChestPayload.TYPE, CheckChestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PullItemsPayload.TYPE, PullItemsPayload.CODEC);

        //Server -> Client
        PayloadTypeRegistry.clientboundPlay().register(OpenPullItemScreenPayload.TYPE, OpenPullItemScreenPayload.CODEC);
    }

    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(CheckChestPayload.TYPE, CheckChestPayload::receiveServer);
        ServerPlayNetworking.registerGlobalReceiver(PullItemsPayload.TYPE, PullItemsPayload::receiveServer);
    }
}
