package dev.client.modules.core.player;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.Formatting;

public class AutoRespawn extends Module {

    public AutoRespawn() {
        super("AutoRespawn", ModuleCategory.Utils, "Автоматически возрождает вас");
    }

    private final CheckBox autoGps = new CheckBox("GPS после смерти", () -> true).defaultValue(false).register(this);
    private final CheckBox notification = new CheckBox("Уведомление о смерти", () -> true).defaultValue(false).register(this);

    private boolean deathProcessed = false;

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        if (mc.currentScreen instanceof DeathScreen) {
            if (!deathProcessed) {
                if (notification.getEnabled()) {
                    String coords = "Координаты: " + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
                    handlerClient().getNotificationManager().add(
                            net.minecraft.text.Text.literal("Вы умерли! " + coords),
                            3,
                            dev.client.api.nullcry.uiClient.notification.type.NotificationType.WARNING,
                            dev.client.api.nullcry.uiClient.notification.type.RenderType.WORLD
                    );
                }
                if (autoGps.getEnabled()) {
                    mc.player.sendMessage(
                            net.minecraft.text.Text.literal(Formatting.RED + "GPS: " + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ()),
                            false
                    );
                }
                deathProcessed = true;
            }
            mc.player.requestRespawn();
            mc.setScreen(null);
        } else {
            deathProcessed = false;
        }
    }
}
