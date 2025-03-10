package com.retrotrack.openitempuller.gui.screen;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.config.ItemPullerConfig;
import com.retrotrack.openitempuller.gui.widget.hover.TextHoverWidget;
import com.retrotrack.openitempuller.networking.payloads.CheckChestPayload;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class SettingsScreen extends Screen {
    //Screen Constants
    protected int backgroundWidth = 259;
    protected int backgroundHeight = 182;
    private static final Identifier TEXTURE = Identifier.of(MOD_ID, "textures/gui/screen/settings_screen.png");
    private int i = (this.width - this.backgroundWidth) / 2;
    private int j = (this.height - this.backgroundHeight) / 2;
    private final Screen parent;

    //Screen Widgets
    private final ArrayList<TextHoverWidget> textHovers = new ArrayList<>();
    private final ArrayList<TextFieldWidget> textFieldWidgets = new ArrayList<>();
    private TexturedButtonWidget pullButton;
    private TexturedButtonWidget settingsButton;
    private ButtonWidget priorityButton;
    private ButtonWidget displayButton;
    private TexturedButtonWidget sortButton;

    //Screen Texts
    private final ArrayList<Text> textWidgets = new ArrayList<>();

    //Variables
    private final int serverRadius;
    private int priorityType = ItemPuller.CONFIG.getInteger("priority_type");
    private int displayMode = ItemPuller.CONFIG.getInteger("display_mode");
    private String sortingMode = ItemPuller.CONFIG.getString("sorting_mode");

    public SettingsScreen(Screen parent, int serverRadius) {
        super(Text.literal(" "));
        this.serverRadius = serverRadius;
        this.parent = parent;
    }

    @Override
    public void close() {
        if (client == null) return;
        saveSettings();
        client.setScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        this.i = (this.width - this.backgroundWidth) / 2;
        this.j = (this.height - this.backgroundHeight) / 2;
        clearWidgets();
        addWidgets();
        addChildren();
    }

    private void clearWidgets(){
        if(!this.children().isEmpty())
            this.clearChildren();
        textFieldWidgets.clear();
        textWidgets.clear();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        if (this.client.world != null) {
            this.renderInGameBackground(context);
            context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, this.i, this.j - 8, 0, 0, this.backgroundWidth, this.backgroundHeight, backgroundWidth, backgroundHeight);
        } else {
            this.renderDarkening(context);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        IntStream.range(0, textWidgets.size()).forEach(k -> context.drawText(textRenderer, textWidgets.get(k),
                this.i + 10, this.height / 2 - (75 - 20 * k), 0xffffff, true));

    }

    private void addChildren() {
        textFieldWidgets.forEach(this::addDrawableChild);
        textHovers.forEach(this::addDrawableChild);
        this.addDrawableChild(settingsButton);
        this.addDrawableChild(pullButton);
        this.addDrawableChild(priorityButton);
        this.addDrawableChild(displayButton);
        this.addDrawableChild(sortButton);
    }

    private void addWidgets() {
        if (this.client == null) return;

        settingsButton = new TexturedButtonWidget(this.i + 217, this.height / 2 - 100, 20, 18,
                new ButtonTextures(Identifier.of(MOD_ID, "button/settings/settings_button_highlighted"),
                        Identifier.of(MOD_ID, "button/settings/settings_button_highlighted")), (button) -> {
            saveSettings();
            client.setScreen(parent);
        });
        pullButton = new TexturedButtonWidget(this.i + 237, this.height / 2 - 100, 20, 18,
                parent instanceof PullItemScreen ? new ButtonTextures(Identifier.of(MOD_ID, "button/pull/pull_button_highlighted"),
                        Identifier.of(MOD_ID, "button/pull/pull_button_highlighted"))
                : new ButtonTextures(Identifier.of(MOD_ID, "button/pull/pull_button"),
                        Identifier.of(MOD_ID, "button/pull/pull_button_highlighted")), (button) -> {
            saveSettings();
            if(parent instanceof PullItemScreen) client.setScreen(parent);
            else ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius")));
        });


        textFieldWidgets.add(new TextFieldWidget(textRenderer,this.i + 86, this.height / 2 - 76,
                40, 14, Text.literal(CONFIG.getString("radius"))));
        textFieldWidgets.getFirst().setText(CONFIG.getString("radius"));
        priorityButton = ButtonWidget.builder(Text.translatable("openitempuller.settings_screen.option_1.mode_" + priorityType), button -> {
                if(priorityType >= 1) priorityType = 0;
                else priorityType++;
                button.setMessage(Text.translatable("openitempuller.settings_screen.option_1.mode_" + priorityType));
                })
                .dimensions(this.i + 85, this.height / 2 - 58, 80, 18)
                .build();

        for (int k = 0; k < 3; k++) {

            TextHoverWidget widget = new TextHoverWidget(this.i + 8, this.height / 2 - (78 - 14 * k), 70, 14);

            widget.setTooltip(Tooltip.of(Text.translatable("openitempuller.settings_screen.option_" + k + ".tooltip", serverRadius)));

            textWidgets.add(Text.translatable("openitempuller.settings_screen.option_" + k));
            textHovers.add(widget);
        }

        sortButton = new TexturedButtonWidget(this.i + 166, this.height / 2 - 58, 18, 18,
                sortingMode.equals("ascending") ? new ButtonTextures(
                        Identifier.of(MOD_ID, "button/sort/sort_button_ascending"),
                        Identifier.of(MOD_ID, "button/sort/sort_button_ascending_highlighted"))
                : new ButtonTextures(
                        Identifier.of(MOD_ID, "button/sort/sort_button_descending"),
                        Identifier.of(MOD_ID, "button/sort/sort_button_descending_highlighted")),
                (button) -> {
                    sortingMode = (sortingMode.equals("ascending") ? "descending" : "ascending");
                    clearWidgets();
                    addWidgets();
                    addChildren();
                });
        displayButton = ButtonWidget.builder(Text.translatable("openitempuller.settings_screen.option_2.mode_" + displayMode), button -> {
                    if(displayMode >= 1) displayMode = 0;
                    else displayMode++;
                    button.setMessage(Text.translatable("openitempuller.settings_screen.option_2.mode_" + displayMode));
                })
                .dimensions(this.i + 85, this.height / 2 - 38, 80, 18)
                .build();
    }

    public void saveSettings() {
        ItemPullerConfig.saveSettings(ItemPuller.getInt(textFieldWidgets.getFirst().getText()) == -1 ?
                (CONFIG.getInteger("radius") == null ? 16 : CONFIG.getInteger("radius")) : ItemPuller.getInt(textFieldWidgets.getFirst().getText()),
                priorityType, sortingMode, displayMode);
    }
}