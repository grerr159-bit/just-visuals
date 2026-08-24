package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.ColorPicker;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.render.core.Draw3DUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class BlockHighLight extends Module {
    public static BlockHighLight INSTANCE;

    public BlockHighLight() {
        super("BlockHighLight", ModuleCategory.Visuals, "Подсвечивает блок на который вы смотрите");
    }

    ModeElement colorMode = new ModeElement("Режим цвета", () -> true)
            .set("Клиентский", "Кастомный")
            .defaultValue("Клиентский")
            .register(this);

    ColorPicker color = new ColorPicker("Цвет", () -> colorMode.isSelected("Кастомный"))
            .set(-1)
            .defaultValue(-1)
            .register(this);

    @Subscribe
    public void onDraw3D(RenderEvent.Draw3D event) {
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = result.getBlockPos();

            int highlightColor;
            if (colorMode.isSelected("Клиентский")) {
                highlightColor = Interface.INSTANCE.getMainColor();
            } else {
                highlightColor = color.getColorRGBA();
            }

            Draw3DUtil.drawShapeAlternative(
                    pos,
                    mc.world.getBlockState(pos).getOutlineShape(mc.world, pos),
                    highlightColor,
                    2,
                    true,
                    true
            );
        }
    }
}
