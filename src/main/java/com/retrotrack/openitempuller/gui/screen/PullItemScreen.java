package com.retrotrack.openitempuller.gui.screen;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.gui.widget.VerticalScrollbarWidget;
import com.retrotrack.openitempuller.gui.widget.hover.TextHoverWidget;
import com.retrotrack.openitempuller.networking.payloads.PullItemsPayload;
import com.retrotrack.openitempuller.util.BlockEntityUtil;
import com.retrotrack.openitempuller.util.RenderUtil;
import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class PullItemScreen extends Screen {

    private final int backgroundWidth = 259;
    private final int backgroundHeight = 182;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/screen/pull_item_screen.png");
    private int i;
    private int j;
    private final Screen parent;

    private int offset = 0;
    private String currentSearch = "";
    private Item selectedItem;

    private ImageButton pullButton;
    private ImageButton settingsButton;
    private final ArrayList<ImageButton> itemSelectButtons = new ArrayList<>();
    private final ArrayList<EditBox> textFieldWidgets = new ArrayList<>();
    private final ArrayList<Checkbox> checkBoxWidgets = new ArrayList<>();
    private final ArrayList<TextHoverWidget> chestsDisplayHovers = new ArrayList<>();
    private EditBox searchBar;
    private VerticalScrollbarWidget scrollbarWidget;

    private final ArrayList<Component> chestDisplayTexts = new ArrayList<>();
    private final ArrayList<Component> itemCountTexts = new ArrayList<>();
    private final ArrayList<Component> itemSelectTexts = new ArrayList<>();

    private final ArrayList<RenderUtil.RenderVariables> renderVariables = new ArrayList<>();
    private final ArrayList<DecodedBlockEntity> decodedBlockEntities;
    private final int serverRadius;
    private final boolean isTargetBlock;

    private ArrayList<DecodedBlockEntity> blockEntitiesWithItem = new ArrayList<>();
    private List<Item> sortedItems;
    private List<Item> filteredList;
    private final HashMap<Item, CompoundTag> pullCompounds = new HashMap<>();
    private BlockPos targetBlock;

    public PullItemScreen(Screen parent, ArrayList<DecodedBlockEntity> decodedBlockEntities, int serverRadius) {
        super(Component.literal(" "));
        this.parent = parent;
        this.decodedBlockEntities = decodedBlockEntities;
        this.serverRadius = serverRadius;
        this.isTargetBlock = CONFIG.getBoolean("is_target_block");
        if (this.isTargetBlock) {
            targetBlock = (BlockPos) CONFIG.getProperty("target_block_pos");
        }
    }

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static String itemName(Item item) {
        return Component.translatable(item.getDescriptionId()).getString();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        this.i = (this.width - this.backgroundWidth) / 2;
        this.j = (this.height - this.backgroundHeight) / 2;
        List<Item> availableItems = new ArrayList<>();

        decodedBlockEntities.forEach(be -> be.items().forEach((item, count) -> {
            if (!availableItems.contains(item)) availableItems.add(item);
        }));
        var base = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .sorted(Comparator.comparing(PullItemScreen::itemName));
        if (CONFIG.getInteger("display_mode") == 0) {
            sortedItems = base.toList();
        } else {
            sortedItems = base.filter(availableItems::contains).toList();
        }
        filteredList = sortedItems;
        searchBar = null;
        scrollbarWidget = null;
        initButtons(-1, true, true);
    }

    private void initButtons(int selectedButton, boolean reloadDisplays, boolean reloadDefault) {
        this.clearWidgets();
        itemSelectButtons.clear();
        itemSelectTexts.clear();
        renderVariables.clear();

        if (selectedItem != null) saveChestSelection();
        if (selectedButton != -1) {
            selectedItem = filteredList.get(selectedButton);
        }
        addDefaultWidgets(reloadDefault);
        if (reloadDisplays) {
            clearDisplays();
            if (selectedItem != null) addChestDisplays();
        }
        this.addButtons();
        this.addChildren();
        this.setFocused(searchBar);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.i, this.j - 8, 0f, 0f,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        IntStream.range(0, itemSelectTexts.size()).forEach(k -> graphics.text(this.font, itemSelectTexts.get(k),
                this.i + 22, this.height / 2 - (75 - 14 * k), 0xFFFFFFFF, true));
        graphics.text(this.font, Component.translatable("openitempuller.pull_screen.top_names"),
                this.i + 105, this.height / 2 - 74, 0xFFFFFFFF, true);
        IntStream.range(0, chestDisplayTexts.size()).forEach(k -> graphics.text(this.font, chestDisplayTexts.get(k),
                this.i + 105, this.height / 2 - (62 - 14 * k), 0xFFFFFFFF, true));
        IntStream.range(0, itemCountTexts.size()).forEach(k -> graphics.text(this.font, itemCountTexts.get(k),
                this.i + 177, this.height / 2 - (62 - 14 * k), 0xFFFFFFFF, true));
        renderVariables.forEach(v -> RenderUtil.renderItemAt(graphics, v));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollbarWidget != null) scrollbarWidget.scrollBy(verticalAmount);
        return true;
    }

    private void filterList() {
        filteredList = sortedItems.stream()
                .filter(item -> itemName(item).toLowerCase().contains(currentSearch.toLowerCase()))
                .collect(Collectors.toList());
        scrollbarWidget.scrollPos = 0;
        offset = 0;
        initButtons(-1, false, false);
    }

    private void addDefaultWidgets(boolean reload) {
        if (this.scrollbarWidget == null || reload) this.scrollbarWidget = new VerticalScrollbarWidget(this.i + 95, this.height / 2 - 78,
                5, 156, 151, Component.literal(""), this::moveItemSelectButtons);
        if (this.searchBar == null || reload) {
            this.searchBar = new EditBox(this.font, this.i + 5, this.height / 2 - 96, 96, 12, Component.literal(""));
            searchBar.setValue(currentSearch);          // set before the responder, so it does not fire
            searchBar.setHint(Component.translatable("openitempuller.pull_screen.search"));
            searchBar.setResponder(text -> {
                currentSearch = text;
                filterList();
            });
        }
        this.addRenderableWidget(this.searchBar);
        this.addRenderableWidget(this.scrollbarWidget);
    }

    private void clearDisplays() {
        chestsDisplayHovers.clear();
        chestDisplayTexts.clear();
        textFieldWidgets.clear();
        checkBoxWidgets.clear();
        itemCountTexts.clear();
    }

    private void addChildren() {
        this.addRenderableWidget(settingsButton);
        this.addRenderableWidget(pullButton);
        itemSelectButtons.forEach(this::addRenderableWidget);
        chestsDisplayHovers.forEach(this::addRenderableWidget);
        textFieldWidgets.forEach(this::addRenderableWidget);
        checkBoxWidgets.forEach(this::addRenderableWidget);
        this.addRenderableWidget(Button.builder(Component.translatable("openitempuller.pull_screen.pull_button"), button -> pullItems())
                .bounds(this.i + 174, this.height / 2 + 83, 80, 20)
                .build());
    }

    private void moveItemSelectButtons(double pos, int scrollHeight) {
        int size = filteredList.size() - 1;
        double percentage = pos / scrollHeight;
        offset = (int) (size * percentage);
        initButtons(-1, false, false);
    }

    private void addButtons() {
        if (this.minecraft == null) return;
        settingsButton = new ImageButton(this.i + 217, this.height / 2 - 100, 20, 18,
                new WidgetSprites(sprite("button/settings/settings_button"), sprite("button/settings/settings_button")),
                button -> {
                    saveChestSelection();
                    this.minecraft.gui.setScreen(new SettingsScreen(this, serverRadius));
                }, CommonComponents.EMPTY);
        pullButton = new ImageButton(this.i + 237, this.height / 2 - 100, 20, 18,
                new WidgetSprites(sprite("button/pull/pull_button_highlighted"), sprite("button/pull/pull_button_highlighted")),
                button -> this.minecraft.gui.setScreen(parent), CommonComponents.EMPTY);

        WidgetSprites textures = new WidgetSprites(sprite("button/item_select/item_select_button"),
                sprite("button/item_select/item_select_button_highlighted"));
        WidgetSprites texturesSelected = new WidgetSprites(sprite("button/item_select/item_select_button_selected"),
                sprite("button/item_select/item_select_button_selected"));

        for (int k = 0; k < Math.min(12, filteredList.size()); k++) {
            int finalK = k;
            if (k + offset >= filteredList.size()) continue;
            Item item = filteredList.get(k + offset);
            ImageButton itemButton = new ImageButton(this.i + 5, this.height / 2 - (78 - 14 * k), 88, (k > 10) ? 2 : 14,
                    item == selectedItem ? texturesSelected : textures,
                    button -> initButtons(finalK + offset, true, false), CommonComponents.EMPTY);
            itemButton.setTooltip(Tooltip.create(Component.translatable(item.getDescriptionId())));
            itemSelectButtons.add(itemButton);
            if (!(k > 10)) {
                itemSelectTexts.add(Component.literal(StringUtils.abbreviate(itemName(item), 12)));
                renderVariables.add(new RenderUtil.RenderVariables(item.getDefaultInstance(), this.i + 13, this.height / 2 - (71 - 14 * k), 0.75f));
            }
        }
    }

    public void pullItems() {
        saveChestSelection();
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
        pullCompounds.forEach((item, compound) -> ClientPlayNetworking.send(new PullItemsPayload(compound)));
    }

    public void saveChestSelection() {
        if (selectedItem == null) return;
        int size = 0;
        CompoundTag compound = new CompoundTag();

        for (int k = 0; k < textFieldWidgets.size(); k++) {
            int count = ItemPuller.getInt(textFieldWidgets.get(k).getValue());
            if (count <= 0) continue;

            BlockPos pos = blockEntitiesWithItem.get(k).pos();
            CompoundTag child = new CompoundTag();
            child.putIntArray("source_pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            child.putInt("id", k);
            child.putString("item", BuiltInRegistries.ITEM.getKey(selectedItem).toString());
            child.putInt("item_count", count);

            compound.put("chest_id_" + size, child);
            size++;
        }
        compound.putInt("size", size);
        compound.putBoolean("from_block", isTargetBlock);
        if (isTargetBlock)
            compound.putIntArray("target_pos", new int[]{targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()});
        pullCompounds.put(selectedItem, compound);
    }

    public HashMap<BlockPos, Integer> loadChestSelection(CompoundTag compound) {
        int size = compound.getIntOr("size", 0);
        HashMap<BlockPos, Integer> chestMap = new HashMap<>();

        for (int k = 0; k < size; k++) {
            CompoundTag child = compound.getCompoundOrEmpty("chest_id_" + k);
            int count = child.getIntOr("item_count", 0);
            int[] p = child.getIntArray("source_pos").orElse(new int[]{0, 0, 0});
            chestMap.put(new BlockPos(p[0], p[1], p[2]), count);
        }
        return chestMap;
    }

    public void addChestDisplays() {
        blockEntitiesWithItem = BlockEntityUtil.getChestsWithItem(selectedItem, decodedBlockEntities);
        HashMap<BlockPos, Integer> chestMap = new HashMap<>();
        if (pullCompounds.get(selectedItem) != null) chestMap = loadChestSelection(pullCompounds.get(selectedItem));
        if (CONFIG.getString("sorting_mode").equals("ascending")) Collections.reverse(blockEntitiesWithItem);

        for (int k = 0; k < blockEntitiesWithItem.size(); k++) {
            if (k >= 10) break;
            DecodedBlockEntity entry = blockEntitiesWithItem.get(k);
            TextHoverWidget widget = new TextHoverWidget(this.i + 105, this.height / 2 - (64 - 14 * k), 65, 14);
            String name = entry.name().contains("translatable")
                    ? Component.translatable(entry.name().replace("translatable: ", "")).getString()
                    : entry.name();
            widget.setTooltip(Tooltip.create(Component.literal(name + " (" + entry.pos().toShortString() + ")")));
            itemCountTexts.add(Component.literal(String.valueOf(entry.items().get(selectedItem))));
            chestsDisplayHovers.add(widget);
            chestDisplayTexts.add(Component.literal(StringUtils.abbreviate(name, 10)));

            EditBox fieldWidget = new EditBox(this.font, this.i + 205, this.height / 2 - (63 - 14 * k),
                    32, 12, Component.literal(""));

            int itemCount = 0;
            boolean isChecked = false;
            Integer stored = chestMap.get(entry.pos());
            if (stored != null) {
                itemCount = stored;
                isChecked = (itemCount == entry.items().get(selectedItem));
            }

            fieldWidget.setValue(String.valueOf(itemCount));
            textFieldWidgets.add(fieldWidget);
            int finalK = k;
            checkBoxWidgets.add(Checkbox.builder(Component.literal(""), this.font)
                    .pos(this.i + 240, this.height / 2 - (63 - 14 * k))
                    .selected(isChecked)
                    .onValueChange((checkbox, checked) -> textFieldWidgets.get(finalK)
                            .setValue(checked ? String.valueOf(blockEntitiesWithItem.get(finalK).items().get(selectedItem)) : ""))
                    .build());
        }
    }
}
