package dev.client.api.nullcry.uiClient.clickGui.components.settings.core.clicksSetting;


import com.google.common.base.Suppliers;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.MouseClick;
import dev.client.api.nullcry.modules.settings.ClickSetting;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.CursorsUtil;
import dev.client.api.nullcry.render.core.animations.client.Direction;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.SettingComponent;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

public class ClickComponent extends SettingComponent {
    private final Supplier<ClickSetting> clickSetting = Suppliers.memoize(() -> (ClickSetting) getSetting());
    boolean hovered;

    public ClickComponent(Setting setting) {
        super(setting);
    }

    @Override
    public void init() {
        float btnH = 14f;
        size(240f / 2f - 10f, btnH + 6f);
    }

    @Override
    public ClickComponent render(DrawContext context, int mouseX, int mouseY, float delta) {
        ClickSetting cs = clickSetting.get();
        float visibility = getGlobalAlpha();

        final float btnH = 14f;
        final float textSize = 7f;

        String text = cs.getDisplayName();
        float tw = ClientApi.inter().getWidth(text, textSize);
        float th = ClientApi.inter().getHeight(text, textSize);

        float bx = getX() + 2;
        float by = getY() + 3f;
        float bw = getWidth() - 10;

        hovered = MouseClick.isClick(mouseX, mouseY, bx, by, bw, btnH);

        if  (Interface.INSTANCE.blurStrength.getValue() > 0) {
            ClientApi.blur()
                    .blurRadius(Math.min(10f, Math.max(0f, Interface.INSTANCE.blurStrength.getValue())))
                    .radius(new QuadRadiusState(3f))
                    .size(new SizeState(bw, btnH))
                    .alpha(visibility)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), bx, by);
        }

        ClientApi.rectangle()
                .size(new SizeState(bw, btnH))
                .color(new QuadColorState(
                        ColorUtils.setAlpha(ColorUtils.rgb(28, 30, 35), (int) (150 * visibility)),
                        ColorUtils.setAlpha(ColorUtils.rgb(24, 26, 30), (int) (150 * visibility)),
                        ColorUtils.setAlpha(ColorUtils.rgb(22, 24, 28), (int) (150 * visibility)),
                        ColorUtils.setAlpha(ColorUtils.rgb(26, 28, 33), (int) (150 * visibility))
                ))
                .radius(new QuadRadiusState(3f))
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), bx, by);


        cs.getAnimation().setMs(250).setValue(1.0);
        cs.getAnimation().setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
        double t = cs.getAnimation().getOutput();
        int rgb = hovered ? -1 : ColorUtils.rgb(180, 180, 180);

        int alpha = (int) Math.round(120 + (255 - 120) * t);
        int textCol = ColorUtils.setAlpha(rgb, Math.round(Math.min(255, Math.max(0, alpha)) * visibility));

        float tx = bx + (bw - tw) / 2f;
        float ty = by + (btnH - th) / 2f - 0.5f;

        ClientApi.text()
                .size(textSize)
                .color(textCol)
                .text(text)
                .font(ClientApi.inter())
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), tx, ty);

        if (hovered) CursorsUtil.setCursor(CursorsUtil.HAND);
        size(getWidth(), btnH + 6f);

        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered) {
            ClickSetting cs = clickSetting.get();
            cs.tryClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}