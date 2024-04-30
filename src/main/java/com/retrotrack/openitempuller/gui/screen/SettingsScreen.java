package com.retrotrack.openitempuller.gui.screen;

import com.retrotrack.openitempuller.ItemPuller;
import com.retrotrack.openitempuller.config.ItemPullerConfig;
import com.retrotrack.openitempuller.networking.ModMessages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

@Environment(EnvType.CLIENT)
public class SettingsScreen extends Screen {
    protected int backgroundWidth = 259;
    protected int backgroundHeight = 182;
    private final Screen parent;
    private static final Identifier TEXTURE = new Identifier(MOD_ID, "textures/gui/screen/settings_screen.png");
    protected int TEXTURE_WIDTH = 259;
    protected int TEXTURE_HEIGHT = 182;

    private TexturedButtonWidget pullButton;
    private ArrayList<Text> textWidgets = new ArrayList<>();
    private ArrayList<TexturedButtonWidget> textHovers = new ArrayList<>();
    private ArrayList<TextFieldWidget> textFieldWidgets = new ArrayList<>();

    private int i;
    private int j;
    private final int serverRadius;
    private ButtonWidget priorityButton;
    private TexturedButtonWidget settingsButton;
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
            context.drawTexture(TEXTURE, this.i, this.j - 8, 0, 0, this.backgroundWidth, this.backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        } else {
            this.renderBackgroundTexture(context);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        IntStream.range(0, textWidgets.size()).forEach(k -> context.drawText(textRenderer, textWidgets.get(k), this.i + 10, this.height / 2 - (75 - 16 * k), 0xffffff, true));

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
                new ButtonTextures(new Identifier(MOD_ID, "button/settings/settings_button_highlighted"), new Identifier(MOD_ID, "button/settings/settings_button_highlighted")), (button) -> {
            saveFiles();
            client.setScreen(parent);
        });
        pullButton = new TexturedButtonWidget(this.i + 237, this.height / 2 - 100, 20, 18,
                parent instanceof PullItemScreen ? new ButtonTextures(new Identifier(MOD_ID, "button/pull/pull_button_highlighted"), new Identifier(MOD_ID, "button/pull/pull_button_highlighted"))
                : new ButtonTextures(new Identifier(MOD_ID, "button/pull/pull_button"), new Identifier(MOD_ID, "button/pull/pull_button_highlighted")), (button) -> {
            saveFiles();
            if(parent instanceof PullItemScreen) client.setScreen(parent);
            else {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(ItemPuller.CONFIG.getInteger("radius"));
                ClientPlayNetworking.send(ModMessages.CHECK_CHESTS, buf);
            }
        });

        priorityButton = ButtonWidget.builder(Text.translatable("open_item_puller.settings_screen.option_1.mode_" + priorityType), button -> {
                if(priorityType >= 1) priorityType = 0;
                else priorityType++;
                button.setMessage(Text.translatable("open_item_puller.settings_screen.option_1.mode_" + priorityType));
                })
                .dimensions(this.i + 85, this.height / 2 - 62, 80, 18)
                .build();
        textFieldWidgets.add(new TextFieldWidget(textRenderer,this.i + 85, this.height / 2 - 78,30, 14, Text.literal(CONFIG.getString("radius"))));
        textFieldWidgets.get(0).setText(CONFIG.getString("radius"));
        for (int k = 0; k < 2; k++) {

            TexturedButtonWidget widget = new TexturedButtonWidget(this.i + 8, this.height / 2 - (78 - 14 * k), 70, 14,
                    new ButtonTextures(new Identifier(MOD_ID, "button/invisible/invisible"),
                            new Identifier(MOD_ID, "button/invisible/invisible")), a -> {});

            widget.setTooltip(Tooltip.of(Text.translatable("open_item_puller.settings_screen.option_" + k + ".tooltip", serverRadius)));

            textWidgets.add(Text.translatable("open_item_puller.settings_screen.option_" + k));
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