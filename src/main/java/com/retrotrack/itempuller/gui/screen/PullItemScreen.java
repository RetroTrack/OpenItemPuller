package com.retrotrack.itempuller.gui.screen;

import com.retrotrack.itempuller.gui.widget.IPCheckboxWidget;
import com.retrotrack.itempuller.networking.ModMessages;
import com.retrotrack.itempuller.util.decoding.DecodedChest;
import com.retrotrack.itempuller.util.decoding.NbtChestCoder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
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
    private ArrayList<Text> itemCountTexts = new ArrayList<>();
    private ArrayList<TexturedButtonWidget> itemSelectButtons = new ArrayList<>();
    private Item selectedItem;
    private ArrayList<Text> itemSelectTexts = new ArrayList<>();
    private ArrayList<Text> chestText = new ArrayList<>();
    private ArrayList<TextFieldWidget> textFieldWidgets = new ArrayList<>();

    private ArrayList<IPCheckboxWidget> checkBoxWidgets = new ArrayList<>();
    private int i;
    private int j;
    private int page = 0;
    private final ArrayList<DecodedChest> decodedChests;
    private boolean isChestDisplayAdded = false;
    private final NbtCompound byteBufNbt;
    private double sliderValue;

    public PullItemScreen(Screen parent, PacketByteBuf buf) {
        super(Text.literal(" "));
        this.byteBufNbt = buf.readNbt();
        this.parent = parent;
        this.
        this.decodedChests = NbtChestCoder.decode(byteBufNbt);
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
        initButtons(-1);
    }

    private void initButtons(int selectedButton){
        this.clearChildren();
        chestsDisplayHovers.clear();
        chestDisplayTexts.clear();
        itemSelectButtons.clear();
        itemSelectTexts.clear();
        itemCountTexts.clear();
        textFieldWidgets.clear();
        checkBoxWidgets.clear();
        if (selectedButton != -1) {
            selectedItem = Registries.ITEM.get(selectedButton + 1);
            addChestDisplays(Registries.ITEM.get(selectedButton + 1));
        } else if (selectedItem != null) addChestDisplays(selectedItem);

        this.addButtons();
        this.addChildren();
        isChestDisplayAdded = false;
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
        IntStream.range(0, itemSelectTexts.size()).forEach(k -> context.drawText(textRenderer, itemSelectTexts.get(k), this.i + 7, this.height / 2 - (75 - 14 * k), 0xffffff, true));
        context.drawText(textRenderer, Text.translatable("item_puller.pull_screen.top_names"), this.i + 105, this.height / 2 - 74, 0xffffff, true);
        IntStream.range(0, chestDisplayTexts.size()).forEach(k -> context.drawText(textRenderer, chestDisplayTexts.get(k), this.i + 105, this.height / 2 - (62 - 14 * k), 0xffffff, true));
        IntStream.range(0, itemCountTexts.size()).forEach(k -> context.drawText(textRenderer, itemCountTexts.get(k), this.i + 177, this.height / 2 - (63 - 14 * k), 0xffffff, true));
        if(!isChestDisplayAdded) {
            chestsDisplayHovers.forEach(this::addDrawableChild);
            textFieldWidgets.forEach(this::addDrawableChild);
            checkBoxWidgets.forEach(this::addDrawableChild);
            isChestDisplayAdded = true;
        }

    }

    private void addChildren() {
        this.addDrawableChild(pullButton);
        itemSelectButtons.forEach(this::addDrawableChild);

        ButtonWidget button1 = ButtonWidget.builder(Text.literal("Pull Items"), button -> {
                    pullItems();
                })
                .dimensions(this.i + 174, this.height / 2 + 83, 80, 20)
                .build();

        ScrollableTextWidget widget = new ScrollableTextWidget(this.i + 100, this.height / 2 + 90, 10, 100, Text.literal(""), textRenderer);
        this.addDrawableChild(widget);
        this.addDrawableChild(button1);
    }
    private void moveItemSelectButtons() {

    }
    private ArrayList<DecodedChest> hasChestItem(Item item) {
        return decodedChests.stream().filter(chest -> chest.items().containsKey(item)).collect(Collectors.toCollection(ArrayList::new));
    }

    private void addButtons() {
        if (this.client == null) return;
        pullButton = new TexturedButtonWidget(this.i + 237, this.height / 2 - 100, 20, 18,
                new ButtonTextures(new Identifier(MOD_ID, "button/pull/pull_button_highlighted"), new Identifier(MOD_ID, "button/pull/pull_button_highlighted")), (button) -> this.close());

        for (int k = 0; k < 12; k++) {
            int finalK = k;
            if(!(k < Registries.ITEM.size() + 1)) continue;
            ButtonTextures textures = new ButtonTextures(new Identifier(MOD_ID, "button/item_select/item_select_button"), new Identifier(MOD_ID, "button/pull/item_select_button_highlighted"),
                    new Identifier(MOD_ID, "button/item_select/item_select_button_highlighted"), new Identifier(MOD_ID, "button/item_select/item_select_button_highlighted"));
            ButtonTextures texturesSelected = new ButtonTextures(new Identifier(MOD_ID, "button/item_select/item_select_button_selected"), new Identifier(MOD_ID, "button/pull/item_select_button_selected"),
                    new Identifier(MOD_ID, "button/item_select/item_select_button_selected"), new Identifier(MOD_ID, "button/item_select/item_select_button_selected"));
            TexturedButtonWidget widget = new TexturedButtonWidget(this.i + 5, this.height / 2 - (78 - 14 * k), 88, (k>10) ? 2 : 14,
                    Registries.ITEM.get(k+1) == selectedItem ? texturesSelected : textures,
                    (button) -> initButtons(finalK));
            widget.setTooltip(Tooltip.of(Text.translatable(Registries.ITEM.get(k+1).getTranslationKey())));
            itemSelectButtons.add(widget);
            if(!(k > 10)) itemSelectTexts.add(Text.literal(StringUtils.abbreviate(Text.translatable(Registries.ITEM.get(k+1).getTranslationKey()).getString(), 16)));
        }
    }

    private void addChestDisplays(Item selectedItem) {
        isChestDisplayAdded = false;
        ArrayList<DecodedChest> chestsWithItem = hasChestItem(selectedItem);
        for (int k = 0; k < chestsWithItem.size(); k++) {
            if(k < 10) {
                TexturedButtonWidget widget = new TexturedButtonWidget(
                        this.i + 105, this.height / 2 - (64 - 14 * k), 65, 14,
                        new ButtonTextures(new Identifier(MOD_ID, "button/invisible/invisible"),
                                new Identifier(MOD_ID, "button/invisible/invisible")), a -> {
                });
                widget.setTooltip(Tooltip.of(Text.literal(chestsWithItem.get(k).name())));
                itemCountTexts.add(Text.literal(String.valueOf(chestsWithItem.get(k).items().get(selectedItem))));
                chestsDisplayHovers.add(widget);
                chestDisplayTexts.add(Text.literal(StringUtils.abbreviate(chestsWithItem.get(k).name(), 10)));
                textFieldWidgets.add(new TextFieldWidget(textRenderer,this.i + 205, this.height / 2 - (64 - 14 * k),32, 12, Text.literal("")));
                int finalK = k;
                checkBoxWidgets.add(IPCheckboxWidget.builder(Text.literal(""), textRenderer).callback((checkbox, checked) -> {
                    textFieldWidgets.get(finalK).setText(checked ? String.valueOf(chestsWithItem.get(finalK).items().get(selectedItem)) :  "");
                }).pos(this.i + 240, this.height / 2 - (64 - 14 * k)).build());
            }
        }
    }


    public void pullItems() {
        this.close();
        int size = 0;
        PacketByteBuf buf = PacketByteBufs.create();
        NbtCompound compound = new NbtCompound();
        for (int k = 0; k < textFieldWidgets.size(); k++) {
            int value = getInt(textFieldWidgets.get(k).getText());
            if (value <= 0) continue;
            size++;
            NbtCompound child = new NbtCompound();
            child.putInt("id", k);
            child.putString("item", Registries.ITEM.getId(selectedItem).toString());
            child.putInt("item_count", value);
            compound.put("chest_" + k, child);
        }
        compound.putInt("size", size);
        compound.put("buf", byteBufNbt);
        buf.writeNbt(compound);
        ClientPlayNetworking.send(ModMessages.PULL_ITEMS, buf);
    }

    public static int getInt(String str) {
        try {return Integer.parseInt(str);}
        catch (NumberFormatException e) {return -1;}
    }
}