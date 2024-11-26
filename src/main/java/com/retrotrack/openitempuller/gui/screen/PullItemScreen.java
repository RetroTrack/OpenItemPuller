package com.retrotrack.openitempuller.gui.screen;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.gui.widget.IPCheckboxWidget;
import com.retrotrack.openitempuller.gui.widget.hover.TextHoverButtonWidget;
import com.retrotrack.openitempuller.gui.widget.VerticalScrollbarWidget;
import com.retrotrack.openitempuller.gui.widget.hover.TextHoverWidget;
import com.retrotrack.openitempuller.networking.payloads.PullItemsPayload;
import com.retrotrack.openitempuller.util.ChestUtil;
import com.retrotrack.openitempuller.util.RenderUtil;
import com.retrotrack.openitempuller.util.decoding.DecodedChest;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class PullItemScreen extends Screen {

    //Screen Constants
    protected final int backgroundWidth = 259;
    protected final int backgroundHeight = 182;
    private static final Identifier TEXTURE = Identifier.of(MOD_ID, "textures/gui/screen/pull_item_screen.png");
    private int i = (this.width - this.backgroundWidth) / 2;
    private int j = (this.height - this.backgroundHeight) / 2;
    private final Screen parent;

    //Screen Variables
    private int offset = 0;
    private String currentSearch = "";
    private Item selectedItem;


    //Screen Widgets
    private TexturedButtonWidget pullButton;
    private final ArrayList<TexturedButtonWidget> itemSelectButtons = new ArrayList<>();
    private final ArrayList<TextFieldWidget> textFieldWidgets = new ArrayList<>();
    private final ArrayList<IPCheckboxWidget> checkBoxWidgets = new ArrayList<>();
    private final ArrayList<TextHoverButtonWidget> chestsDisplayHovers = new ArrayList<>();
    private TextFieldWidget searchBar;
    private VerticalScrollbarWidget scrollbarWidget;

    //Screen Texts
    private final ArrayList<Text> chestDisplayTexts = new ArrayList<>();
    private final ArrayList<Text> itemCountTexts = new ArrayList<>();
    private final ArrayList<Text> itemSelectTexts = new ArrayList<>();
    private TexturedButtonWidget settingsButton;

    //Constants
    private final ArrayList<RenderUtil.RenderVariables> renderVariables = new ArrayList<>();
    private final ArrayList<DecodedChest> decodedChests;
    private final int serverRadius;

    //Variables
    private ArrayList<DecodedChest> chestsWithItem = new ArrayList<>();
    private List<Item> sortedItems;
    private List<Item> filteredList;
    private HashMap<Item, NbtCompound> pullCompounds = new HashMap<>();


    public PullItemScreen(Screen parent, ArrayList<DecodedChest> decodedChests, int serverRadius) {
        super(Text.literal(" "));
        this.parent = parent;
        this.decodedChests = decodedChests;
        this.serverRadius = serverRadius;
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
        List<Item> availableItems = new ArrayList<>();
        decodedChests.forEach(decodedChest -> decodedChest.items().forEach((item, id) -> {
            if (!availableItems.contains(item)) availableItems.add(item);
        }));
        if(CONFIG.getInteger("display_mode") == 0) {
            sortedItems = Registries.ITEM.stream()
                    .filter(item -> item != Items.AIR)
                    .sorted(Comparator.comparing(item -> Text.translatable(item.getTranslationKey()).getString()))
                    .toList();
        }else {
            sortedItems = Registries.ITEM.stream()
                    .filter(item -> item != Items.AIR)
                    .sorted(Comparator.comparing(item -> Text.translatable(item.getTranslationKey()).getString()))
                    .filter(availableItems::contains)
                    .toList();
        }
        filteredList = sortedItems;
        initButtons(-1, true, true);
    }

    private void initButtons(int selectedButton, boolean reloadDisplays, boolean reloadDefault){
        this.clearChildren();
        itemSelectButtons.clear();
        itemSelectTexts.clear();
        renderVariables.clear();

        if(selectedItem != null) saveChestSelection();
        if (selectedButton != -1) {
            selectedItem = filteredList.get(selectedButton);
        }
        addDefaultWidgets(reloadDefault);
        if(reloadDisplays) {
            clearDisplays();
            if (selectedItem != null) addChestDisplays();
        }
        this.addButtons();
        this.addChildren();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        if (this.client.world != null) {
            this.renderInGameBackground(context);

            context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, this.i, this.j - 8, 0, 0, this.backgroundWidth, this.backgroundHeight, backgroundWidth, backgroundHeight);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        IntStream.range(0, itemSelectTexts.size()).forEach(k -> context.drawText(textRenderer, itemSelectTexts.get(k),
                this.i + 22, this.height / 2 - (75 - 14 * k), 0xffffff, true));
        context.drawText(textRenderer, Text.translatable("openitempuller.pull_screen.top_names"), this.i + 105, this.height / 2 - 74, 0xffffff, true);
        IntStream.range(0, chestDisplayTexts.size()).forEach(k -> context.drawText(textRenderer, chestDisplayTexts.get(k),
                this.i + 105, this.height / 2 - (62 - 14 * k), 0xffffff, true));
        IntStream.range(0, itemCountTexts.size()).forEach(k -> context.drawText(textRenderer, itemCountTexts.get(k),
                this.i + 177, this.height / 2 - (62 - 14 * k), 0xffffff, true));
        renderVariables.forEach(RenderUtil::renderItemAt);
    }

    private void filterList() {
        filteredList = sortedItems.stream()
                .filter(item -> Text.translatable(item.getTranslationKey()).getString().toLowerCase().contains(currentSearch.toLowerCase()))
                .collect(Collectors.toList());
        initButtons(-1, false, true);
        scrollbarWidget.scrollPos = 0;
        moveItemSelectButtons(0, 100);
    }

    private void addDefaultWidgets(boolean reload) {
        if(this.scrollbarWidget == null || reload) this.scrollbarWidget = new VerticalScrollbarWidget(this.i + 95, this.height / 2 - 78,
                5, 156, 151, Text.literal(""), (this::moveItemSelectButtons));
        if(this.searchBar == null || reload)
            this.searchBar = new TextFieldWidget(textRenderer, this.i + 5, this.height / 2 - 96, 96, 12, Text.literal("")) {
                @Override
                public boolean charTyped(char chr, int modifiers) {
                    super.charTyped(chr, modifiers);
                    if (!this.isActive()) return false;

                    currentSearch = this.getText();
                    filterList();
                    return true;
                }

                @Override
                public void eraseCharacters(int characterOffset) {
                    super.eraseCharacters(characterOffset);
                    currentSearch = this.getText();
                    filterList();
                }
            };
        searchBar.setPlaceholder(Text.translatable("openitempuller.pull_screen.search"));
        searchBar.setText(currentSearch);
        this.addDrawableChild(this.searchBar);
        this.addDrawableChild(this.scrollbarWidget);
    }

    private void clearDisplays() {
        chestsDisplayHovers.clear();
        chestDisplayTexts.clear();
        textFieldWidgets.clear();
        checkBoxWidgets.clear();
        itemCountTexts.clear();
    }

    private void addChildren() {
        this.addDrawableChild(settingsButton);
        this.addDrawableChild(pullButton);
        itemSelectButtons.forEach(this::addDrawableChild);
        chestsDisplayHovers.forEach(this::addDrawableChild);
        textFieldWidgets.forEach(this::addDrawableChild);
        checkBoxWidgets.forEach(this::addDrawableChild);
        ButtonWidget button1 = ButtonWidget.builder(Text.translatable("openitempuller.pull_screen.pull_button"), button -> pullItems())
                .dimensions(this.i + 174, this.height / 2 + 83, 80, 20)
                .build();
        this.addDrawableChild(button1);
    }
    private void moveItemSelectButtons(double pos, int scrollHeight) {
        int size = filteredList.size() - 1;
        double percentage = pos/scrollHeight;
        offset = (int) (size * percentage);

        initButtons(-1, false, false);
    }

    private void addButtons() {
        if (this.client == null) return;
        settingsButton = new TexturedButtonWidget(this.i + 217, this.height / 2 - 100, 20, 18,
                new ButtonTextures(Identifier.of(MOD_ID, "button/settings/settings_button"),
                        Identifier.of(MOD_ID, "button/settings/settings_button")), (button) -> {
            saveChestSelection();
            client.setScreen(new SettingsScreen(this, serverRadius));
        });
        pullButton = new TexturedButtonWidget(this.i + 237, this.height / 2 - 100, 20, 18,
                new ButtonTextures(Identifier.of(MOD_ID, "button/pull/pull_button_highlighted"),
                        Identifier.of(MOD_ID, "button/pull/pull_button_highlighted")), (button) -> client.setScreen(parent));

        for (int k = 0; k < Math.min(12, filteredList.size()); k++) {
            int finalK = k;
            if(k+ offset >= filteredList.size()) continue;
            Item item = filteredList.get(k + offset);
            ButtonTextures textures =
                    new ButtonTextures(Identifier.of(MOD_ID, "button/item_select/item_select_button"), Identifier.of(MOD_ID, "button/pull/item_select_button_highlighted"),
                    Identifier.of(MOD_ID, "button/item_select/item_select_button_highlighted"), Identifier.of(MOD_ID, "button/item_select/item_select_button_highlighted"));
            ButtonTextures texturesSelected =
                    new ButtonTextures(Identifier.of(MOD_ID, "button/item_select/item_select_button_selected"), Identifier.of(MOD_ID, "button/pull/item_select_button_selected"),
                    Identifier.of(MOD_ID, "button/item_select/item_select_button_selected"), Identifier.of(MOD_ID, "button/item_select/item_select_button_selected"));
            TexturedButtonWidget itemButton = new TexturedButtonWidget(this.i + 5, this.height / 2 - (78 - 14 * k), 88, (k > 10) ? 2 : 14,
                    item == selectedItem ? texturesSelected : textures,
                    (button) -> initButtons(finalK + offset, true, false)) {
                @Override
                public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
                    scrollbarWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                    return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                }
            };
            itemButton.setTooltip(Tooltip.of(Text.translatable(item.getTranslationKey())));
            itemSelectButtons.add(itemButton);
            if (!(k > 10)) {
                itemSelectTexts.add(Text.literal(StringUtils.abbreviate(Text.translatable(item.getTranslationKey()).getString(), 12)));
                renderVariables.add(new RenderUtil.RenderVariables(item.getDefaultStack(), this.i + 13, this.height / 2 - (71 - 14 * k), 0.75f, client.getItemRenderer()));
            }
        }
    }

//    private void addChestDisplays() {
//        chestsWithItem = ChestUtil.getChestsWithItem(selectedItem, decodedChests);
//        if(CONFIG.getString("sorting_mode").equals("ascending")) Collections.reverse(chestsWithItem);
//
//        for (int k = 0; k < chestsWithItem.size(); k++) {
//            if (k >= 10) break;
//            TextHoverWidget widget = new TextHoverWidget(
//                    this.i + 105, this.height / 2 - (64 - 14 * k), 65, 14);
//            widget.setTooltip(Tooltip.of(Text.literal(chestsWithItem.get(k).name())));
//            itemCountTexts.add(Text.literal(String.valueOf(chestsWithItem.get(k).items().get(selectedItem))));
//            chestsDisplayHovers.add(widget);
//            chestDisplayTexts.add(Text.literal(StringUtils.abbreviate(chestsWithItem.get(k).name(), 10)));
//            textFieldWidgets.add(new TextFieldWidget(textRenderer,
//                    this.i + 205,
//                    this.height / 2 - (63 - 14 * k),
//                    32,
//                    12,
//                    Text.literal("")));
//            int finalK = k;
//            checkBoxWidgets.add(IPCheckboxWidget.builder(Text.literal(""), textRenderer)
//                    .callback((checkbox, checked) -> textFieldWidgets.get(finalK).setText(checked ? String.valueOf(chestsWithItem.get(finalK).items().get(selectedItem)) :  ""))
//                    .pos(this.i + 240, this.height / 2 - (63 - 14 * k)).build());
//        }
//    }

    public void pullItems() {
        if (client != null) client.setScreen(parent);
        saveChestSelection();
        pullCompounds.forEach((item, compound) -> ClientPlayNetworking.send(new PullItemsPayload(compound)));
    }

    public void saveChestSelection() {
        int size = 0;
        NbtCompound compound = new NbtCompound();

        for (int k = 0; k < textFieldWidgets.size(); k++) {
            int count = ItemPuller.getInt(textFieldWidgets.get(k).getText());
            if (count <= 0) continue;

            NbtCompound child = new NbtCompound();
                child.putIntArray("pos", new int[]{chestsWithItem.get(k).pos().getX(), chestsWithItem.get(k).pos().getY(), chestsWithItem.get(k).pos().getZ()});
                child.putInt("id", k);
                child.putString("item", Registries.ITEM.getId(selectedItem).toString());
                child.putInt("item_count", count);

            compound.put("chest_id_" + size, child);
            size++;
        }
        compound.putInt("size", size);
        pullCompounds.put(selectedItem, compound);

    }

    public HashMap<BlockPos, Integer> loadChestSelection(NbtCompound compound) {
        int size = compound.getInt("size");
        HashMap<BlockPos, Integer> chestMap = new HashMap<>();

        for (int k = 0; k < size; k++) {
            NbtCompound child = compound.getCompound("chest_id_" + k);
            int count = child.getInt("item_count");
            BlockPos pos = new BlockPos(child.getIntArray("pos")[0], child.getIntArray("pos")[1], child.getIntArray("pos")[2]);
            chestMap.put(pos, count);
        }
        return chestMap;
    }

    public void addChestDisplays() {

        chestsWithItem = ChestUtil.getChestsWithItem(selectedItem, decodedChests);
        HashMap<BlockPos, Integer> chestMap = new HashMap<>();
        if(pullCompounds.get(selectedItem) != null) chestMap = loadChestSelection(pullCompounds.get(selectedItem));
        if(CONFIG.getString("sorting_mode").equals("ascending")) Collections.reverse(chestsWithItem);

        for (int k = 0; k < chestsWithItem.size(); k++) {
            if (k >= 10) break;
            TextHoverWidget widget = new TextHoverWidget(
                    this.i + 105, this.height / 2 - (64 - 14 * k), 65, 14);
            widget.setTooltip(Tooltip.of(Text.literal(chestsWithItem.get(k).name())));
            itemCountTexts.add(Text.literal(String.valueOf(chestsWithItem.get(k).items().get(selectedItem))));
            chestsDisplayHovers.add(widget);
            chestDisplayTexts.add(Text.literal(StringUtils.abbreviate(chestsWithItem.get(k).name(), 10)));

            TextFieldWidget fieldWidget = new TextFieldWidget(textRenderer,
                    this.i + 205, this.height / 2 - (63 - 14 * k),
                    32, 12, Text.literal(""));

            int finalK = k;
            int itemCount = 0;
            boolean isChecked = false;
            if(chestMap.get(chestsWithItem.get(finalK).pos()) != null) {
                itemCount = chestMap.get(chestsWithItem.get(finalK).pos());
                isChecked = (itemCount == chestsWithItem.get(finalK).items().get(selectedItem));
            }

            fieldWidget.setText(String.valueOf(itemCount));
            textFieldWidgets.add(fieldWidget);
            checkBoxWidgets.add(IPCheckboxWidget.builder(Text.literal(""), textRenderer)
                    .callback((checkbox, checked) -> textFieldWidgets.get(finalK)
                            .setText(checked ? String.valueOf(chestsWithItem.get(finalK).items().get(selectedItem)) :  ""))
                    .pos(this.i + 240, this.height / 2 - (63 - 14 * k))
                    .checked(isChecked).build());

        }
    }
}