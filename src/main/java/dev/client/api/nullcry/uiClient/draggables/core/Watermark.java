package dev.client.api.nullcry.uiClient.draggables.core;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.client.ConnectionHelper;
import dev.client.api.nullcry.helper.client.PlayerHelper;
import dev.client.api.nullcry.helper.other.DraggableHandler;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.SelectElements;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingProvider;
import dev.client.api.nullcry.uiClient.draggables.GlassShadow;
import dev.client.api.nullcry.uiClient.draggables.HelperElements;
import dev.client.api.nullcry.uiClient.draggables.IHelper;
import dev.client.api.nullcry.uiClient.draggables.settings.DraggableSettingsPanel;
import dev.client.api.nullcry.uiClient.draggables.settings.PanelAlphaProvider;
import dev.client.modules.core.render.Interface;

import java.util.ArrayList;
import java.util.List;

public class Watermark implements IHelper, SettingProvider, PanelAlphaProvider {
    final DraggableHandler draggableHandler;
    private final List<Setting> settings = new ArrayList<>();
    public final SelectElements watermarkElements = new SelectElements("Элементы Watermark", () -> true)
            .set("Login", "Server")
            .defaultValue("Login")
            .register(this);

    public final CheckBox showFps = new CheckBox("Показывать FPS", () -> true).defaultValue(true).register(this);
    public final CheckBox showPing = new CheckBox("Показывать Ping", () -> true).defaultValue(true).register(this);
    public final Slider textSize = new Slider("Размер текста", () -> true).set(4f, 16f, 0.5f).applyDefault(8f).register(this);
    public final CheckBox shortName = new CheckBox("Сокращенное название", () -> true).defaultValue(true).register(this);

    public final ColorPicker bgColor = new ColorPicker("Цвет фона", () -> true)
            .defaultValue(0x00000000)
            .register(this);

    public final Slider bgAlpha = new Slider("Прозрачность фона", () -> true)
            .set(0f, 255f, 1f)
            .applyDefault(180f)
            .register(this);

    public final Slider borderRadius = new Slider("Скругление углов", () -> true)
            .set(0f, 20f, 0.5f)
            .applyDefault(4f)
            .register(this);

    public final CheckBox glass = new CheckBox("Стекло", () -> true)
            .defaultValue(false)
            .register(this);

    public Watermark() {
        this.draggableHandler = addDraggable("Watermark", 4, 4);
        this.draggableHandler.setActiveCondition(() -> {
            return dev.client.modules.core.hud.HudModuleHelper.isWatermarkEnabled();
        });
        this.draggableHandler.setSettingsPanel(new DraggableSettingsPanel(draggableHandler, this));
    }

    float width;
    float height;

    final float ANIMATION_SPEED = 40;

    float displayedFps = -1;
    float displayedPing = -1;

    long lastFpsUpdateTime = 0;
    long lastPingUpdateTime = 0;

    @Override
    public void onRender(RenderEvent.Draw2D event) {
        final boolean showName = watermarkElements.isSelected("Login");
        final boolean showFps = this.showFps.getEnabled();
        final boolean showPing = this.showPing.getEnabled();
        final boolean showServer = watermarkElements.isSelected("Server");

        float x = draggableHandler.getX();
        float y = draggableHandler.getY();

        int fpsTarget = mc.getCurrentFps();
        int pingTarget;
        try {
            pingTarget = Integer.parseInt(PlayerHelper.getLocalPing());
        } catch (Exception ignored) {
            pingTarget = 0;
        }

        if (displayedFps < 0) displayedFps = fpsTarget;
        if (displayedPing < 0) displayedPing = pingTarget;

        displayedFps = HelperElements.smoothAnimation(ANIMATION_SPEED, displayedFps, fpsTarget, lastFpsUpdateTime);
        displayedPing = HelperElements.smoothAnimation(ANIMATION_SPEED, displayedPing, pingTarget, lastPingUpdateTime);

        long now = System.currentTimeMillis();
        lastFpsUpdateTime = now;
        lastPingUpdateTime = now;

        String displayText = "Just visuals";
        if (showFps) displayText += " | " + Math.round(displayedFps) + " fps";
        if (showPing) displayText += " | " + Math.round(displayedPing) + "ms";
        if (showName) displayText += " | " + mc.player.getName().getString();
        if (showServer) displayText += " | " + ConnectionHelper.getServerIP();

        float textSize = this.textSize.getValue();
        float paddingX = 12f;
        float paddingY = 5f;

        float textWidth = ClientApi.inter().getWidth(displayText, textSize);
        float textHeight = ClientApi.inter().getHeight(displayText, textSize);

        width = textWidth + paddingX * 2;
        height = textHeight + paddingY * 2;

        draggableHandler.setWidth(width);
        draggableHandler.setHeight(height);

        float radius = borderRadius.getValue();
        int bgColorInt = bgColor.getColorRGBA();
        boolean glassEnabled = glass.getEnabled();
        int bgA = glassEnabled ? 0 : bgAlpha.getValue().intValue();

        float s = 1f;
        float normalizedAlpha = Math.min(1f, bgA / 255f);

        if (glassEnabled) {
            float outlineExpand = 2f;
            ClientApi.blur()
                    .blurRadius(50f)
                    .Smoothness(4f)
                    .radius(new QuadRadiusState(radius + outlineExpand))
                    .size(new SizeState(width + outlineExpand * 2, height + outlineExpand * 2))
                    .alpha(s * 0.6f)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x - outlineExpand, y - outlineExpand);

            ClientApi.blur()
                    .blurRadius(50f)
                    .Smoothness(4f)
                    .radius(new QuadRadiusState(radius))
                    .size(new SizeState(width, height))
                    .alpha(s)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x, y);

            GlassShadow.render(event.getContext().getMatrices().peek().getPositionMatrix(), x, y, width, height, radius, s);
        }

        if (bgA > 0) {
            int outlineAlpha = (int) (255 * s * normalizedAlpha);
            int outlineColor = ColorUtils.setAlpha(Interface.INSTANCE.getMainColor(), outlineAlpha);

            ClientApi.rectangle()
                    .size(new SizeState(width, height))
                    .color(new QuadColorState(outlineColor))
                    .radius(new QuadRadiusState(radius))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x, y);

            float innerPad = 1f;
            int fillColor = ColorUtils.setAlpha(bgColorInt, (int) (bgA * s));

            ClientApi.rectangle()
                    .size(new SizeState(width - innerPad * 2, height - innerPad * 2))
                    .color(new QuadColorState(fillColor))
                    .radius(new QuadRadiusState(Math.max(0f, radius - innerPad)))
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), x + innerPad, y + innerPad);
        }

        float textX = x + paddingX - 2f;
        float textY = y + (height - textHeight) / 2f;

        ClientApi.text()
                .text(displayText).size(textSize).font(ClientApi.inter())
                .color(-1)
                .build()
                .render(event.getContext().getMatrices().peek().getPositionMatrix(), textX, textY);
    }

    @Override
    public List<Setting> getSettings() {
        return settings;
    }

    @Override
    public float getPanelAlpha() {
        return 1f;
    }
}
