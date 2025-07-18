package com.retrotrack.openitempuller.event;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.config.ItemPullerConfig;
import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import com.retrotrack.openitempuller.util.BlockEntitySearchUtil;
import com.retrotrack.openitempuller.util.BlockRaycastHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;


@Environment(EnvType.CLIENT)
public class KeyInputHandler {
    public static final String KEY_CATEGORY_OPENITEMPULLER = "openitempuller.key.category.openitempuller";
    public static final String KEY_SET_TARGET_BLOCK = "openitempuller.key.set_target_block";
    public static final String KEY_REMOVE_TARGET_BLOCK = "openitempuller.key.remove_target_block";
    public static final String KEY_OPEN_PULL_SCREEN = "openitempuller.key.open_pull_screen";

    public static KeyBinding setTargetKey;
    public static KeyBinding removeTargetKey;
    public static KeyBinding openPullScreenKey;

    public static void registerKeyInputs(){
        ClientTickEvents.END_CLIENT_TICK.register(KeyInputHandler::handleKeyInputTick);
    }

    public static void register() {
        setTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(KEY_SET_TARGET_BLOCK, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, KEY_CATEGORY_OPENITEMPULLER));
        removeTargetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(KEY_REMOVE_TARGET_BLOCK, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, KEY_CATEGORY_OPENITEMPULLER));
        openPullScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(KEY_OPEN_PULL_SCREEN, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, KEY_CATEGORY_OPENITEMPULLER));

        registerKeyInputs();
    }

    private static void handleKeyInputTick(MinecraftClient client) {
        if (client.player == null) return;
        if (setTargetKey.isPressed()) {
            setTargetBlock(client);
            return;
        }
        if (removeTargetKey.isPressed()) {
            removeTargetBlock(client);
            return;
        }
        if (openPullScreenKey.isPressed()) {
            ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius")));
        }
    }

    private static void removeTargetBlock(MinecraftClient client) {
        ItemPuller.CONFIG.putProperty("is_target_block", false);
        ItemPullerConfig.saveConfig(ItemPuller.CONFIG, ItemPullerConfig.CONFIG_FILE);
        client.player.sendMessage(Text.translatable("openitempuller.message.remove_target_block").formatted(Formatting.RED), true);
    }

    private static void setTargetBlock(MinecraftClient client) {
        //Sets target to block player is currently looking at and turn on block targeting
        BlockPos blockPos = BlockRaycastHelper.getBlockPlayerIsLookingAt(client.player);
        if (client.world != null && blockPos != null && BlockEntitySearchUtil.isValidBlockEntity(blockPos, client.world)) {
            ItemPuller.CONFIG.putProperty("target_block_pos", blockPos);
            ItemPuller.CONFIG.putProperty("is_target_block", true);
            ItemPullerConfig.saveConfig(ItemPuller.CONFIG, ItemPullerConfig.CONFIG_FILE);

            client.player.sendMessage(Text.translatable("openitempuller.message.set_target_block.success", blockPos.toShortString()).formatted(Formatting.GREEN), true);
        }
    }
}
