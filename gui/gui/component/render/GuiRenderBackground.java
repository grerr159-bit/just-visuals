package ru.night.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import ru.night.ui.gui.GuiScreen;
import ru.night.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public class GuiRenderBackground extends GuiScreen {
   public static void renderBackground(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backGroundOneColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(178.5F * mainAlpha));
      int blur = Renderer2D.ColorUtil.replAlpha(new Color(255, 255, 255).getRGB(), (int)(255.0F * mainAlpha));
      renderer2D.rectOutline(GuiScreen.x, GuiScreen.y, GuiScreen.width, GuiScreen.height, 6.5F, outlineColor, 0.5F);
      if (mainAlpha > 0.1 && GuiScreen.clientBlurSetting.get()) {
         renderer2D.prepareBlur(45.0F);
         renderer2D.blur(GuiScreen.x, GuiScreen.y, GuiScreen.width, GuiScreen.height, 6.5F, mainAlpha);
      }

      renderer2D.rect(GuiScreen.x, GuiScreen.y, GuiScreen.width, GuiScreen.height, 6.5F, backGroundOneColor);
   }
}
