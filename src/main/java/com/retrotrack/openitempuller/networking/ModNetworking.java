package com.retrotrack.openitempuller.networking;

import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import com.retrotrack.openitempuller.networking.payloads.OpenPullItemScreenPayload;
import com.retrotrack.openitempuller.networking.payloads.OpenSettingsScreenPayload;
import com.retrotrack.openitempuller.networking.payloads.PullItemsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModNetworking {

    public static void registerCommonPackets() {
        //Client -> Server
        PayloadTypeRegistry.playC2S().register(CheckChestPayload.ID, CheckChestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenSettingsScreenPayload.ID, OpenSettingsScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PullItemsPayload.ID, PullItemsPayload.CODEC);

        //Server -> Client
        PayloadTypeRegistry.playS2C().register(OpenPullItemScreenPayload.ID, OpenPullItemScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenSettingsScreenPayload.ID, OpenSettingsScreenPayload.CODEC);
    }

    public static void registerServerPackets() {
        //Client -> Server
        ServerPlayNetworking.registerGlobalReceiver(CheckChestPayload.ID, CheckChestPayload::receiveServer);
        ServerPlayNetworking.registerGlobalReceiver(OpenSettingsScreenPayload.ID, OpenSettingsScreenPayload::receiveServer);
        ServerPlayNetworking.registerGlobalReceiver(PullItemsPayload.ID, PullItemsPayload::receiveServer);
    }

    public static void registerClientPackets() {
        //Server -> Client
        ClientPlayNetworking.registerGlobalReceiver(OpenPullItemScreenPayload.ID, OpenPullItemScreenPayload::receiveClient);
        ClientPlayNetworking.registerGlobalReceiver(OpenSettingsScreenPayload.ID, OpenSettingsScreenPayload::receiveClient);
    }
}
