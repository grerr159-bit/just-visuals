package dev.client.api.nullcry.uiClient.clickGui.components.settings.core.selectElementsSetting;

import com.google.common.base.Suppliers;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.modules.settings.SelectElements;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

public class SelectElementsComponent extends SettingComponent {
    private final Supplier<SelectElements> modeListSetting = Suppliers.memoize(() -> (SelectElements) getSetting());
    boolean isHovered;

    public SelectElementsComponent(Setting setting) {
        super(setting);
    }

    @Override
    public void init() {
        float nameH = ClientApi.inter().getHeight(getSetting().getName(), 7f) + 5f;
        size(240f / 2f - 10f, nameH + 12f);
    }

    @Override
    public SelectElementsComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        SelectElements sel = modeListSetting.get();
        float visibility = getGlobalAlpha();

        float nameSize = 7f;
        float nameH = ClientApi.inter().getHeight(getSetting().getName(), nameSize);
        ClientApi.text()
                .size(nameSize)
                .color(ColorUtils.setAlpha(
                        -1,
                        Math.round(ColorUtils.getAlpha(-1) * visibility)
                ))
                .text(getSetting().getName())
                .font(ClientApi.inter())
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), getX(), getY() - 1f);

        final float chipGapX = 2f;
        final float chipGapY = 2f;
        final float chipH = 12f;
        final float padX = 5f;
        final float textSize = 6f;

        float startX = getX() - 1f;
        float x = startX;
        float y = getY() + nameH + 3f;
        float maxWidth = getX() + getWidth() - 4;

        int theme = -1;
        int mainColor = Interface.INSTANCE.getMainColor();
        float minAlpha = 45f;
        float maxAlpha = 150f;

        boolean anyHover = false;
        float lastBottom = y + chipH;

        for (String val : sel.asStringList()) {
            String t = (val == null ? "N/A" : val);
            float tw = ClientApi.inter().getWidth(t, textSize);
            float chipW = tw + padX * 2f;

            if (x + chipW > maxWidth) {
                x = startX;
                y += chipH + chipGapY;
            }

            boolean selected = sel.isSelected(t);
            boolean hovered = MouseClick.isClick(mouseX, mouseY, x, y, chipW, chipH);
            float state = selected ? 1f : (hovered ? 0.5f : 0f);
            int backgroundAlpha = Math.round((minAlpha + (maxAlpha - minAlpha) * state) * visibility);
            backgroundAlpha = Math.max(0, Math.min(255, backgroundAlpha));

            if (Interface.INSTANCE.blurStrength.getValue() > 0) {
                ClientApi.blur()
                        .blurRadius(Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue())))
                        .radius(new QuadRadiusState(2f))
                        .size(new SizeState(chipW, chipH))
                        .alpha(visibility)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), x, y);
            }

            ClientApi.rectangle()
                    .size(new SizeState(chipW, chipH))
                    .color(new QuadColorState(
                            ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), backgroundAlpha),
                            ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), backgroundAlpha),
                            ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), backgroundAlpha),
                            ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), backgroundAlpha)
                    ))
                    .radius(new QuadRadiusState(2f))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), x, y);

            int textBase = selected ? theme : ColorUtils.rgb(180, 180, 180);
            int textColor = ColorUtils.setAlpha(
                    textBase,
                    Math.round(ColorUtils.getAlpha(textBase) * visibility)
            );

            float tx = (float) Math.round(x + (chipW - tw) / 2f);
            float ty = y + (chipH - ClientApi.inter().getHeight(t, textSize)) / 2f - 0.5f;

            ClientApi.text()
                    .size(textSize)
                    .color(textColor)
                    .text(t)
                    .font(ClientApi.inter())
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), tx, ty);

            isHovered = hovered;
            if (isHovered) anyHover = true;

            x += chipW + chipGapX;
            lastBottom = y + chipH;
        }

        if (anyHover) CursorsUtil.setCursor(CursorsUtil.HAND);

        size(getWidth(), (lastBottom + 2f) - getY());
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SelectElements sel = modeListSetting.get();

        final float chipGapX = 2f, chipGapY = 2f, chipH = 12f, padX = 5f, textSize = 6f;
        float nameH = ClientApi.inter().getHeight(getSetting().getName(), 7f);

        float startX = getX() - 1f;
        float x = startX;
        float y = getY() + nameH + 3f;
        float maxWidth = getX() + getWidth() - 4;

        for (String val : sel.asStringList()) {
            String t = (val == null ? "N/A" : val);
            float tw = ClientApi.inter().getWidth(t, textSize);
            float chipW = tw + padX * 2f;

            if (x + chipW > maxWidth) {
                x = startX;
                y += chipH + chipGapY;
            }

            if (MouseClick.isClick(mouseX, mouseY, x, y, chipW, chipH)) {
                if (val != null) {
                    boolean cur = sel.isSelected(val);
                    sel.get(val).applyDefault(!cur);
                }
                return true;
            }

            x += chipW + chipGapX;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
