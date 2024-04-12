package com.retrotrack.itempuller.mixin;

import com.retrotrack.itempuller.networking.ModMessages;
import com.retrotrack.itempuller.util.ChestFinder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static com.retrotrack.itempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> {


    @Final
    @Shadow
    private RecipeBookWidget recipeBook;

    @Shadow
    private boolean narrow;
    @Shadow
    private boolean mouseDown;

    @Unique
    private TexturedButtonWidget pullButton;

    public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Override
    public void init() {
        if (this.client == null) return;
        assert this.client.interactionManager != null;
        if (this.client.interactionManager.hasCreativeInventory()) {
            if (this.client.player == null) return;
            this.client.setScreen(new CreativeInventoryScreen(this.client.player, this.client.player.networkHandler.getEnabledFeatures(), this.client.options.getOperatorItemsTab().getValue()));
        } else {
            super.init();
            this.narrow = this.width < 379;
            this.recipeBook.initialize(this.width, this.height, this.client, this.narrow, this.handler);
            this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
            renderPullButton();
            this.addDrawableChild(new TexturedButtonWidget(this.x + 104, this.height / 2 - 22, 20, 18, RecipeBookWidget.BUTTON_TEXTURES, (button) -> {
                this.recipeBook.toggleOpen();
                this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
                button.setPosition(this.x + 104, this.height / 2 - 22);
                this.mouseDown = true;
                getPullButton().setPosition(this.x + 154, this.height / 2 - 100);
            }));
            this.addSelectableChild(this.recipeBook);
            this.setInitialFocus(this.recipeBook);
        }
    }

    @Unique
    private void renderPullButton() {
        if (this.client == null) return;
        setPullButton(this.addDrawableChild(new TexturedButtonWidget(this.x + 154, this.height / 2 - 100, 20, 18,
                new ButtonTextures(new Identifier(MOD_ID, "button/pull/pull_button"/*default*/), new Identifier(MOD_ID, "button/pull/pull_button_highlighted"/*highlighted*/)), (button) -> {
            button.setPosition(this.x + 154, this.height / 2 - 100);
            assert this.client.player != null;
            //Send Check Chest Packet
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt( 128);
            buf.writeInt(this.x + 154);
            ClientPlayNetworking.send(ModMessages.CHECK_CHESTS, buf);
            this.mouseDown = true;
        })));
    }

    @Unique
    public TexturedButtonWidget getPullButton() {
        return pullButton;
    }

    @Unique
    public void setPullButton(TexturedButtonWidget pullButton) {
        this.pullButton = pullButton;
    }
}
