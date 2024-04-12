package com.retrotrack.itempuller.gui.screen;

import com.retrotrack.itempuller.util.decoding.DecodedChest;
import com.retrotrack.itempuller.util.decoding.NbtChestCoder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.retrotrack.itempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class PullItemScreen extends Screen {
    protected int backgroundWidth = 259;
    protected int backgroundHeight = 166;
    private final Screen parent;
    private static final Identifier TEXTURE = new Identifier(MOD_ID, "textures/gui/screen/pull_item_screen.png");
    protected int TEXTURE_WIDTH = 259;
    protected int TEXTURE_HEIGHT = 166;

    private TexturedButtonWidget pullButton;
    private ArrayList<TexturedButtonWidget> chestsDisplayHovers = new ArrayList<>();
    private ArrayList<Text> chestDisplayTexts = new ArrayList<>();
    private ArrayList<TexturedButtonWidget> itemSelectButtons = new ArrayList<>();
    private ArrayList<Boolean> itemSelectButtonSelected = new ArrayList<>();
    private ArrayList<Text> itemSelectTexts = new ArrayList<>();
    private ArrayList<Text> chestText = new ArrayList<>();
    private int i;
    private int j;
    private int page = 0;
    private final ArrayList<DecodedChest> decodedChests;
    private boolean isChestDisplayAdded = false;

    public PullItemScreen(Screen parent, PacketByteBuf buf) {
        super(Text.literal(" "));
        this.parent = parent;
        this.decodedChests = NbtChestCoder.decode(buf);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        this.i = (this.width - this.backgroundWidth) / 2;
        this.j = (this.height - this.backgroundHeight) / 2;
        chestsDisplayHovers.clear();
        itemSelectButtons.clear();
        itemSelectButtonSelected.clear();
        itemSelectTexts.clear();
        isChestDisplayAdded = false;
        this.addButtons();
        this.addChildren();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        if (this.client.world != null) {
            this.renderInGameBackground(context);
            context.drawTexture(TEXTURE, this.i, this.j, 0, 0, this.backgroundWidth, this.backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        } else {
            this.renderBackgroundTexture(context);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        IntStream.range(0, itemSelectTexts.size()).forEach(k -> context.drawTextWithShadow(textRenderer, itemSelectTexts.get(k), this.i + 7, this.height / 2 - (75 - 14 * k), 0xffffff));

        IntStream.range(0, chestDisplayTexts.size()).forEach(k -> context.drawTextWithShadow(textRenderer, chestDisplayTexts.get(k), this.i + 105, this.height / 2 - (75 - 14 * k), 0xffffff));
        if(!isChestDisplayAdded) {
            chestsDisplayHovers.forEach(this::addDrawableChild);
            isChestDisplayAdded = true;
        }
    }

    private void addChildren() {
        itemSelectButtons.forEach(this::addDrawableChild);
        this.addDrawableChild(pullButton);

    }
    private ArrayList<DecodedChest> hasChestItem(Item item) {
        return decodedChests.stream().filter(chest -> chest.uniqueItems().contains(item)).collect(Collectors.toCollection(ArrayList::new));
    }

    private void addButtons() {
        if (this.client == null) return;
        pullButton = new TexturedButtonWidget(this.i + 237, this.height / 2 - 100, 20, 18,
                new ButtonTextures(new Identifier(MOD_ID, "button/pull/pull_button_highlighted"), new Identifier(MOD_ID, "button/pull/pull_button_highlighted")), (button) -> this.close());

        for (int k = 0; k < 12; k++) {
            int finalK = k;
            TexturedButtonWidget widget = new TexturedButtonWidget(this.i + 5, this.height / 2 - (78 - 14 * k), 88, (k>10) ? 2 : 14,
                    new ButtonTextures(new Identifier(MOD_ID, "button/item_select/item_select_button"), new Identifier(MOD_ID, "button/pull/item_select_button_highlighted"),
                            new Identifier(MOD_ID, "button/item_select/item_select_button_selected"), new Identifier(MOD_ID, "button/item_select/item_select_button_selected")),
                    (button) -> setItemSelectButtonSelected(finalK));
            widget.setTooltip(Tooltip.of(Text.translatable(Registries.ITEM.get(k+1).getTranslationKey())));
            itemSelectButtons.add(widget);
            itemSelectButtonSelected.add(false);
            if(!(k > 10) && k < Registries.ITEM.size() + 1) itemSelectTexts.add(Text.literal(StringUtils.abbreviate(Text.translatable(Registries.ITEM.get(k+1).getTranslationKey()).getString(), 16)));
        }
    }

    private void addChestDisplays(Item selectedItem) {

        isChestDisplayAdded = false;
        client.player.sendMessage(Text.literal(selectedItem.toString()));
        chestsDisplayHovers.clear();
        chestDisplayTexts.clear();
        ArrayList<DecodedChest> chestsWithItem = hasChestItem(selectedItem);
        for (int k = 0; k < chestsWithItem.size(); k++) {
            if(k < 10) {
                client.player.sendMessage(Text.literal(chestsWithItem.get(k).name()));
                TexturedButtonWidget widget = new TexturedButtonWidget(
                        this.i + 105, this.height / 2 - (78 - 14 * k), 65, 14,
                        new ButtonTextures(new Identifier(MOD_ID, "button/invisible/invisible"),
                                new Identifier(MOD_ID, "button/invisible/invisible")), a -> {
                });
                widget.setTooltip(Tooltip.of(Text.literal(chestsWithItem.get(k).name())));

                chestsDisplayHovers.add(widget);
                chestDisplayTexts.add(Text.literal(StringUtils.abbreviate(chestsWithItem.get(k).name(), 10)));
            }
        }
    }

    private void setItemSelectButtonSelected(int id) {
        itemSelectButtons.get(id).active = false;
        itemSelectButtonSelected.set(id, true);
        addChestDisplays(Registries.ITEM.get(id+1));
        for (int k = 0; k < itemSelectButtons.size(); k++) {
            if(itemSelectButtonSelected.get(k)){
                itemSelectButtonSelected.set(k, false);
                itemSelectButtons.get(k).active = true;
            }
        }
    }
}