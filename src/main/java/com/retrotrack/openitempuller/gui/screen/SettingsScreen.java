package com.retrotrack.openitempuller.gui.screen;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.config.ItemPullerConfig;
import com.retrotrack.openitempuller.gui.widget.hover.TextHoverWidget;
import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class SettingsScreen extends Screen {
    private final int backgroundWidth = 259;
    private final int backgroundHeight = 182;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/screen/settings_screen.png");
    private int i;
    private int j;
    private final Screen parent;

    private final ArrayList<TextHoverWidget> textHovers = new ArrayList<>();
    private final ArrayList<EditBox> textFieldWidgets = new ArrayList<>();
    private ImageButton pullButton;
    private ImageButton settingsButton;
    private Button priorityButton;
    private Button displayButton;
    private ImageButton sortButton;

    private final ArrayList<Component> textWidgets = new ArrayList<>();

    private final int serverRadius;
    private int priorityType = ItemPuller.CONFIG.getInteger("priority_type");
    private int displayMode = ItemPuller.CONFIG.getInteger("display_mode");
    private String sortingMode = ItemPuller.CONFIG.getString("sorting_mode");

    public SettingsScreen(Screen parent, int serverRadius) {
        super(Component.literal(" "));
        this.serverRadius = serverRadius;
        this.parent = parent;
    }

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onClose() {
        if (this.minecraft == null) return;
        saveSettings();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        this.i = (this.width - this.backgroundWidth) / 2;
        this.j = (this.height - this.backgroundHeight) / 2;
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        textHovers.clear();
        textFieldWidgets.clear();
        textWidgets.clear();
        addWidgets();
        addChildren();
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
        IntStream.range(0, textWidgets.size()).forEach(k -> graphics.text(this.font, textWidgets.get(k),
                this.i + 10, this.height / 2 - (75 - 20 * k), 0xFFFFFFFF, true));
    }

    private void addChildren() {
        textFieldWidgets.forEach(this::addRenderableWidget);
        textHovers.forEach(this::addRenderableWidget);
        this.addRenderableWidget(settingsButton);
        this.addRenderableWidget(pullButton);
        this.addRenderableWidget(priorityButton);
        this.addRenderableWidget(displayButton);
        this.addRenderableWidget(sortButton);
    }

    private void addWidgets() {
        if (this.minecraft == null) return;

        settingsButton = new ImageButton(this.i + 217, this.height / 2 - 100, 20, 18,
                new WidgetSprites(sprite("button/settings/settings_button_highlighted"),
                        sprite("button/settings/settings_button_highlighted")),
                button -> {
                    saveSettings();
                    this.minecraft.gui.setScreen(parent);
                }, CommonComponents.EMPTY);

        WidgetSprites pullSprites = parent instanceof PullItemScreen
                ? new WidgetSprites(sprite("button/pull/pull_button_highlighted"), sprite("button/pull/pull_button_highlighted"))
                : new WidgetSprites(sprite("button/pull/pull_button"), sprite("button/pull/pull_button_highlighted"));
        pullButton = new ImageButton(this.i + 237, this.height / 2 - 100, 20, 18, pullSprites, button -> {
            saveSettings();
            if (parent instanceof PullItemScreen) this.minecraft.gui.setScreen(parent);
            else ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius")));
        }, CommonComponents.EMPTY);

        EditBox radiusBox = new EditBox(this.font, this.i + 86, this.height / 2 - 76,
                40, 14, Component.literal(CONFIG.getString("radius")));
        radiusBox.setValue(CONFIG.getString("radius"));
        textFieldWidgets.add(radiusBox);

        priorityButton = Button.builder(Component.translatable("openitempuller.settings_screen.option_1.mode_" + priorityType), button -> {
                    if (priorityType >= 1) priorityType = 0;
                    else priorityType++;
                    button.setMessage(Component.translatable("openitempuller.settings_screen.option_1.mode_" + priorityType));
                })
                .bounds(this.i + 85, this.height / 2 - 58, 80, 18)
                .build();

        for (int k = 0; k < 3; k++) {
            TextHoverWidget widget = new TextHoverWidget(this.i + 8, this.height / 2 - (78 - 14 * k), 70, 14);
            widget.setTooltip(Tooltip.create(Component.translatable("openitempuller.settings_screen.option_" + k + ".tooltip", serverRadius)));
            textWidgets.add(Component.translatable("openitempuller.settings_screen.option_" + k));
            textHovers.add(widget);
        }

        sortButton = new ImageButton(this.i + 166, this.height / 2 - 58, 18, 18,
                sortingMode.equals("ascending")
                        ? new WidgetSprites(sprite("button/sort/sort_button_ascending"), sprite("button/sort/sort_button_ascending_highlighted"))
                        : new WidgetSprites(sprite("button/sort/sort_button_descending"), sprite("button/sort/sort_button_descending_highlighted")),
                button -> {
                    sortingMode = sortingMode.equals("ascending") ? "descending" : "ascending";
                    rebuild();
                }, CommonComponents.EMPTY);

        displayButton = Button.builder(Component.translatable("openitempuller.settings_screen.option_2.mode_" + displayMode), button -> {
                    if (displayMode >= 1) displayMode = 0;
                    else displayMode++;
                    button.setMessage(Component.translatable("openitempuller.settings_screen.option_2.mode_" + displayMode));
                })
                .bounds(this.i + 85, this.height / 2 - 38, 80, 18)
                .build();
    }

    public void saveSettings() {
        int typed = ItemPuller.getInt(textFieldWidgets.getFirst().getValue());
        Integer current = CONFIG.getInteger("radius");
        ItemPullerConfig.saveSettings(typed == -1 ? (current == null ? 16 : current) : typed,
                priorityType, sortingMode, displayMode);
    }
}
