package com.retrotrack.openitempuller.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.config.ItemPullerConfig;
import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import com.retrotrack.openitempuller.util.BlockRaycastHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class KeyInputHandler {
    // Translation key becomes "key.category.openitempuller.main" (see en_us.json)
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final String KEY_SET_TARGET_BLOCK = "openitempuller.key.set_target_block";
    public static final String KEY_REMOVE_TARGET_BLOCK = "openitempuller.key.remove_target_block";
    public static final String KEY_OPEN_PULL_SCREEN = "openitempuller.key.open_pull_screen";

    public static KeyMapping setTargetKey;
    public static KeyMapping removeTargetKey;
    public static KeyMapping openPullScreenKey;

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register(KeyInputHandler::handleKeyInputTick);
    }

    public static void register() {
        setTargetKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_SET_TARGET_BLOCK, InputConstants.KEY_Y, CATEGORY));
        removeTargetKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_REMOVE_TARGET_BLOCK, InputConstants.KEY_U, CATEGORY));
        openPullScreenKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_OPEN_PULL_SCREEN, InputConstants.KEY_O, CATEGORY));

        registerKeyInputs();
    }

    private static void handleKeyInputTick(Minecraft client) {
        if (client.player == null) return;
        // original behaviour: key is checked while held (isPressed -> isDown)
        if (setTargetKey.isDown()) {
            setTargetBlock(client);
            return;
        }
        if (removeTargetKey.isDown()) {
            removeTargetBlock(client);
            return;
        }
        if (openPullScreenKey.isDown()) {
            ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius")));
        }
    }

    private static void removeTargetBlock(Minecraft client) {
        ItemPuller.CONFIG.putProperty("is_target_block", false);
        ItemPullerConfig.saveConfig(ItemPuller.CONFIG, ItemPullerConfig.CONFIG_FILE);
        client.player.sendSystemMessage(Component.translatable("openitempuller.message.remove_target_block").withStyle(ChatFormatting.RED));
    }

    private static void setTargetBlock(Minecraft client) {
        // Sets target to block player is currently looking at and turn on block targeting
        BlockPos blockPos = BlockRaycastHelper.getBlockPlayerIsLookingAt(client.player);
        if (client.level != null && blockPos != null && BlockEntitySearchUtil.isValidBlockEntity(blockPos, client.level)) {
            ItemPuller.CONFIG.putProperty("target_block_pos", blockPos);
            ItemPuller.CONFIG.putProperty("is_target_block", true);
            ItemPullerConfig.saveConfig(ItemPuller.CONFIG, ItemPullerConfig.CONFIG_FILE);

            client.player.sendSystemMessage(Component.translatable("openitempuller.message.set_target_block.success", blockPos.toShortString()).withStyle(ChatFormatting.GREEN));
        }
    }
}
