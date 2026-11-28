package com.retrotrack.openitempuller.gui.screen;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.client.ItemPullerClient;
import com.retrotrack.openitempuller.config.ClientConfig;
import com.retrotrack.openitempuller.gui.widget.VerticalScrollbarWidget;
import com.retrotrack.openitempuller.gui.widget.hover.TextHoverWidget;
import com.retrotrack.openitempuller.networking.ClientNetworkUtil;
import com.retrotrack.openitempuller.networking.payloads.PullItemsPayload;
import com.retrotrack.openitempuller.util.BlockEntityUtil;
import com.retrotrack.openitempuller.util.RenderUtil;
import com.retrotrack.openitempuller.util.decoding.DecodedBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.Minecraft;
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
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class PullItemScreen extends Screen {

    private final int backgroundWidth = 259;
    private final int backgroundHeight = 182;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/screen/pull_item_screen.png");
    private int i;
    private int j;
    private final Screen parent;

    /** Number of chest rows the layout has room for. */
    private static final int CHEST_ROWS = 10;

    private int offset = 0;
    /** First chest (in {@link #blockEntitiesWithItem}) that should be shown; changed by scrolling over the chest list. */
    private int chestOffset = 0;
    /** First chest the current widgets actually show. Differs from chestOffset only while a rebuild is under way. */
    private int shownChestOffset = 0;
    private Component chestRangeText;
    private String currentSearch = "";
    /** The selected entry of the item list: an item together with its components (enchantments, name, contents, ...). */
    private ItemVariant selectedItem;

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
    private final int serverMaxRadius;

    private ArrayList<DecodedBlockEntity> blockEntitiesWithItem = new ArrayList<>();
    private List<ItemVariant> sortedItems;
    private List<ItemVariant> filteredList;
    /** Display name per entry, computed once per init instead of on every keystroke / comparison. */
    private final HashMap<ItemVariant, String> variantNames = new HashMap<>();
    /** Vanilla tooltip per entry. Building one is the expensive part of a scroll step, so it is done once. */
    private final HashMap<ItemVariant, Component> variantTooltips = new HashMap<>();
    /** What {@link #sortedItems} was built for: only a change of display mode needs a rebuild, not a resize. */
    private ClientConfig.DisplayMode sortedItemsMode;
    /** What the player filled in per item variant; sent together as one packet when pulling. */
    private final LinkedHashMap<ItemVariant, PullItemsPayload.Entry> pullSelections = new LinkedHashMap<>();
    /** Storage to pull into, or null to pull into the player inventory. */
    private final BlockPos targetBlock;

    public PullItemScreen(Screen parent, ArrayList<DecodedBlockEntity> decodedBlockEntities, int serverMaxRadius) {
        super(Component.literal(" "));
        this.parent = parent;
        this.decodedBlockEntities = decodedBlockEntities;
        this.serverMaxRadius = serverMaxRadius;
        this.targetBlock = ItemPullerClient.usableTarget(Minecraft.getInstance(), serverMaxRadius);
    }

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private String itemName(ItemVariant variant) {
        return variantNames.computeIfAbsent(variant, v -> v.toStack().getHoverName().getString());
    }

    /** The vanilla tooltip of the stack (enchantments, lore, ...), so variants of one item can be told apart. */
    private Component variantTooltip(ItemVariant variant) {
        Component cached = variantTooltips.get(variant);
        if (cached != null) return cached;
        if (this.minecraft == null || this.minecraft.level == null) return Component.literal(itemName(variant));
        List<Component> lines = variant.toStack().getTooltipLines(
                Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, TooltipFlag.NORMAL);
        MutableComponent joined = Component.empty();
        for (int k = 0; k < lines.size(); k++) {
            if (k > 0) joined.append("\n");
            joined.append(lines.get(k));
        }
        variantTooltips.put(variant, joined);
        return joined;
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
        // The sorted list of every entry is the costly part (it translates and sorts the whole item registry),
        // and it only depends on the display mode, so a window resize reuses it.
        ClientConfig.DisplayMode mode = ItemPullerClient.config().displayMode();
        if (sortedItems == null || sortedItemsMode != mode) {
            // What is in the chests, per variant: plain, enchanted, renamed or filled stacks are separate entries
            Set<ItemVariant> shown = new LinkedHashSet<>();
            if (mode == ClientConfig.DisplayMode.ALL) {
                BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).forEach(item -> shown.add(ItemVariant.of(item)));
            }
            decodedBlockEntities.forEach(be -> shown.addAll(be.items().keySet()));
            variantNames.clear();
            shown.forEach(this::itemName);
            sortedItems = shown.stream().sorted(Comparator.comparing(this::itemName)).toList();
            sortedItemsMode = mode;
        }
        // The search box keeps its text across a resize / returning from settings, so filter to match it
        filteredList = applyFilter();
        offset = 0;
        chestOffset = 0;
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
            ItemVariant picked = filteredList.get(selectedButton);
            if (!picked.equals(selectedItem)) chestOffset = 0;
            selectedItem = picked;
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
        if (chestRangeText != null) {
            graphics.text(this.font, chestRangeText, this.i + 105, this.height / 2 + 89, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Right of the item list the wheel scrolls the chest list; elsewhere it scrolls the item list.
        if (mouseX >= this.i + 100 && selectedItem != null) scrollChests(verticalAmount);
        else if (scrollbarWidget != null) scrollbarWidget.scrollBy(verticalAmount);
        return true;
    }

    private void scrollChests(double verticalAmount) {
        int max = Math.max(0, blockEntitiesWithItem.size() - CHEST_ROWS);
        int next = Mth.clamp(chestOffset - (int) Math.signum(verticalAmount), 0, max);
        if (next == chestOffset) return;
        chestOffset = next;
        initButtons(-1, true, false); // saves the visible amounts first, then shows the new rows
    }

    private List<ItemVariant> applyFilter() {
        if (currentSearch.isEmpty()) return sortedItems;
        String needle = currentSearch.toLowerCase();
        return sortedItems.stream()
                .filter(item -> itemName(item).toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }

    private void filterList() {
        filteredList = applyFilter();
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
        chestRangeText = null;
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
        int newOffset = (int) (size * percentage);
        if (newOffset == offset) return; // moved less than one row: nothing to rebuild
        offset = newOffset;
        initButtons(-1, false, false);
    }

    private void addButtons() {
        if (this.minecraft == null) return;
        settingsButton = new ImageButton(this.i + 217, this.height / 2 - 100, 20, 18,
                new WidgetSprites(sprite("button/settings/settings_button"), sprite("button/settings/settings_button")),
                button -> {
                    saveChestSelection();
                    this.minecraft.gui.setScreen(new SettingsScreen(this, serverMaxRadius));
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
            ItemVariant item = filteredList.get(k + offset);
            ImageButton itemButton = new ImageButton(this.i + 5, this.height / 2 - (78 - 14 * k), 88, (k > 10) ? 2 : 14,
                    item.equals(selectedItem) ? texturesSelected : textures,
                    button -> initButtons(finalK + offset, true, false), CommonComponents.EMPTY);
            itemButton.setTooltip(Tooltip.create(variantTooltip(item)));
            itemSelectButtons.add(itemButton);
            if (!(k > 10)) {
                itemSelectTexts.add(Component.literal(StringUtils.abbreviate(itemName(item), 12)));
                renderVariables.add(new RenderUtil.RenderVariables(item.toStack(), this.i + 13, this.height / 2 - (71 - 14 * k), 0.75f));
            }
        }
    }

    public void pullItems() {
        saveChestSelection();
        if (this.minecraft == null) return;
        // Taken before leaving the screen: the registry is needed to measure how big the packet gets
        RegistryAccess registryAccess = this.minecraft.level != null ? this.minecraft.level.registryAccess() : null;
        List<PullItemsPayload.Entry> entries = pullSelections.values().stream()
                .filter(entry -> !entry.sources().isEmpty())
                .toList();
        this.minecraft.gui.setScreen(parent);
        if (entries.isEmpty() || registryAccess == null) return;

        // One click is one packet (more only when huge item components force a split)
        for (PullItemsPayload packet : PullItemsPayload.split(Optional.ofNullable(targetBlock), entries, registryAccess)) {
            // Stop after the first failure so the "server has no mod" message is shown only once
            if (!ClientNetworkUtil.trySend(packet)) break;
        }
    }

    public void saveChestSelection() {
        if (selectedItem == null) return;
        // Start from what was stored before, so chests that are scrolled out of view keep their amounts;
        // then apply the rows that are on screen now.
        PullItemsPayload.Entry previous = pullSelections.get(selectedItem);
        LinkedHashMap<BlockPos, Integer> amounts = previous == null
                ? new LinkedHashMap<>() : loadChestSelection(previous);
        for (int k = 0; k < textFieldWidgets.size(); k++) {
            BlockPos pos = blockEntitiesWithItem.get(shownChestOffset + k).pos();
            int count = ItemPuller.getInt(textFieldWidgets.get(k).getValue());
            if (count > 0) amounts.put(pos, count);
            else amounts.remove(pos);
        }
        List<PullItemsPayload.Source> sources = amounts.entrySet().stream()
                .limit(PullItemsPayload.MAX_SOURCES)
                .map(e -> new PullItemsPayload.Source(e.getKey(), e.getValue()))
                .toList();
        pullSelections.put(selectedItem, new PullItemsPayload.Entry(selectedItem, sources));
    }

    public LinkedHashMap<BlockPos, Integer> loadChestSelection(PullItemsPayload.Entry selection) {
        LinkedHashMap<BlockPos, Integer> chestMap = new LinkedHashMap<>();
        for (PullItemsPayload.Source source : selection.sources()) chestMap.put(source.pos(), source.count());
        return chestMap;
    }

    public void addChestDisplays() {
        blockEntitiesWithItem = BlockEntityUtil.getChestsWithItem(selectedItem, decodedBlockEntities,
                ItemPullerClient.config().priorityType());
        HashMap<BlockPos, Integer> chestMap = new HashMap<>();
        if (pullSelections.get(selectedItem) != null) chestMap = loadChestSelection(pullSelections.get(selectedItem));
        if (ItemPullerClient.config().sortingMode() == ClientConfig.SortingMode.ASCENDING) Collections.reverse(blockEntitiesWithItem);

        int total = blockEntitiesWithItem.size();
        chestOffset = Mth.clamp(chestOffset, 0, Math.max(0, total - CHEST_ROWS));
        shownChestOffset = chestOffset;
        int rows = Math.min(CHEST_ROWS, total - chestOffset);

        for (int row = 0; row < rows; row++) {
            int index = chestOffset + row;
            DecodedBlockEntity entry = blockEntitiesWithItem.get(index);
            TextHoverWidget widget = new TextHoverWidget(this.i + 105, this.height / 2 - (64 - 14 * row), 65, 14);
            // Default names are translatable and resolve in the client's language; custom names are literal
            String name = entry.name().getString();
            widget.setTooltip(Tooltip.create(Component.literal(name + " (" + entry.pos().toShortString() + ")")));
            itemCountTexts.add(Component.literal(String.valueOf(entry.items().get(selectedItem))));
            chestsDisplayHovers.add(widget);
            chestDisplayTexts.add(Component.literal(StringUtils.abbreviate(name, 10)));

            EditBox fieldWidget = new EditBox(this.font, this.i + 205, this.height / 2 - (63 - 14 * row),
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
            int finalRow = row;
            checkBoxWidgets.add(Checkbox.builder(Component.literal(""), this.font)
                    .pos(this.i + 240, this.height / 2 - (63 - 14 * row))
                    .selected(isChecked)
                    .onValueChange((checkbox, checked) -> textFieldWidgets.get(finalRow)
                            .setValue(checked ? String.valueOf(entry.items().get(selectedItem)) : ""))
                    .build());
        }
        // Only when there is more than fits: "11-20 / 37". The mouse wheel over the chest list scrolls it.
        chestRangeText = total > CHEST_ROWS
                ? Component.translatable("openitempuller.pull_screen.chest_range", chestOffset + 1, chestOffset + rows, total)
                : null;
    }
}
