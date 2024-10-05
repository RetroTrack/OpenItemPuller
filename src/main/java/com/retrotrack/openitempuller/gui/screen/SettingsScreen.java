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

    //Screen Texts
    private final ArrayList<Text> textWidgets = new ArrayList<>();

    //Variables
    private final int serverRadius;
    private int priorityType = ItemPuller.CONFIG.getInteger("priority_type");

    public SettingsScreen(Screen parent, int serverRadius) {
        super(Text.literal(" "));
        this.serverRadius = serverRadius;
        this.parent = parent;
    }

    @Override
    public void close() {
        if (client == null) return;
        saveFiles();
        client.setScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        this.i = (this.width - this.backgroundWidth) / 2;
        this.j = (this.height - this.backgroundHeight) / 2;
        initButtons();
    }

    private void initButtons(){
        this.clearChildren();
        textFieldWidgets.clear();
        textWidgets.clear();
        this.addWidgets();
        this.addChildren();

    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        if (this.client.world != null) {
            this.renderInGameBackground(context);
            context.drawTexture(TEXTURE, this.i, this.j - 8, 0, 0, this.backgroundWidth, this.backgroundHeight, backgroundWidth, backgroundHeight);
        } else {
            this.renderDarkening(context);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        IntStream.range(0, textWidgets.size()).forEach(k -> context.drawText(textRenderer, textWidgets.get(k),
                this.i + 10, this.height / 2 - (75 - 16 * k), 0xffffff, true));

    }

    private void addChildren() {
        textFieldWidgets.forEach(this::addDrawableChild);
        textHovers.forEach(this::addDrawableChild);
        this.addDrawableChild(settingsButton);
        this.addDrawableChild(pullButton);
        this.addDrawableChild(priorityButton);
    }

    private void addWidgets() {
        if (this.client == null) return;

        settingsButton = new TexturedButtonWidget(this.i + 217, this.height / 2 - 100, 20, 18,
                new ButtonTextures(Identifier.of(MOD_ID, "button/settings/settings_button_highlighted"),
                        Identifier.of(MOD_ID, "button/settings/settings_button_highlighted")), (button) -> {
            saveFiles();
            client.setScreen(parent);
        });
        pullButton = new TexturedButtonWidget(this.i + 237, this.height / 2 - 100, 20, 18,
                parent instanceof PullItemScreen ? new ButtonTextures(Identifier.of(MOD_ID, "button/pull/pull_button_highlighted"),
                        Identifier.of(MOD_ID, "button/pull/pull_button_highlighted"))
                : new ButtonTextures(Identifier.of(MOD_ID, "button/pull/pull_button"),
                        Identifier.of(MOD_ID, "button/pull/pull_button_highlighted")), (button) -> {
            saveFiles();
            if(parent instanceof PullItemScreen) client.setScreen(parent);
            else {
                ClientPlayNetworking.send(new CheckChestPayload(ItemPuller.CONFIG.getInteger("radius")));
            }
        });

        priorityButton = ButtonWidget.builder(Text.translatable("openitempuller.settings_screen.option_1.mode_" + priorityType), button -> {
                if(priorityType >= 1) priorityType = 0;
                else priorityType++;
                button.setMessage(Text.translatable("openitempuller.settings_screen.option_1.mode_" + priorityType));
                })
                .dimensions(this.i + 85, this.height / 2 - 62, 80, 18)
                .build();
        textFieldWidgets.add(new TextFieldWidget(textRenderer,this.i + 85, this.height / 2 - 78,
                30, 14, Text.literal(CONFIG.getString("radius"))));
        textFieldWidgets.get(0).setText(CONFIG.getString("radius"));
        for (int k = 0; k < 2; k++) {

            TextHoverWidget widget = new TextHoverWidget(this.i + 8, this.height / 2 - (78 - 14 * k), 70, 14);

            widget.setTooltip(Tooltip.of(Text.translatable("openitempuller.settings_screen.option_" + k + ".tooltip", serverRadius)));

            textWidgets.add(Text.translatable("openitempuller.settings_screen.option_" + k));
            textHovers.add(widget);
        }
    }

    public void saveFiles() {
        CONFIG.addProperty("radius", getInt(textFieldWidgets.get(0).getText()) == -1 ?
                (CONFIG.getInteger("radius") == null ? 16 : CONFIG.getInteger("radius")) : getInt(textFieldWidgets.get(0).getText()));
        CONFIG.addProperty("priority_type", priorityType);
        ItemPullerConfig.saveConfig(CONFIG, ItemPullerConfig.CONFIG_FILE);
    }

    public static int getInt(String str) {
        try {return Integer.parseInt(str);}
        catch (NumberFormatException e) {return -1;}
    }
}