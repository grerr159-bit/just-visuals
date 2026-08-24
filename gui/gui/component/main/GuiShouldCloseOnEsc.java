package ru.night.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.night.ui.gui.GuiScreen;
import ru.night.util.render.math.animation.anim.util.Easings;

@Environment(EnvType.CLIENT)
public class GuiShouldCloseOnEsc extends GuiScreen {
   public static boolean shouldCloseOnEsc() {
      if (!GuiScreen.exit && GuiScreen.alphaPC.getValue() > 0.0) {
         GuiScreen.alphaPC.run(0.0, 0.4F, Easings.CIRC_OUT);
         GuiScreen.exit = true;
      }

      return false;
   }
}
