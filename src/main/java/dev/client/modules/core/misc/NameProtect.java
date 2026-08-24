package dev.client.modules.core.misc;

import dev.client.Just;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.CheckBox;
import dev.client.api.nullcry.modules.settings.Input;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class NameProtect extends Module {
    public static NameProtect INSTANCE;

    public NameProtect() {
        super("NameProtect", ModuleCategory.Utils, "Скрывает имена игроков");
    }

    public CheckBox friend = new CheckBox("Скрывать на друзьях", () -> true).defaultValue(true).register(this);
    public CheckBox anarchy = new CheckBox("Скрывать на анархии", () -> true).defaultValue(true).register(this);
    public Input anarchyInput = new Input("Замена анархии", () -> anarchy.getEnabled()).set("1").register(this);
    public final String nameClient = "t.me/justvisuals";

    public static String getDisplayName(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            if (entity == mc.player && NameProtect.INSTANCE.isEnabled()) {
                return NameProtect.INSTANCE.nameClient;
            }
            if (Just.getInstance().getFriendManager().isFriend(player.getName().getString()) && NameProtect.INSTANCE.isEnabled()) {
                return NameProtect.INSTANCE.nameClient;
            }
        }
        return entity.getName().getString();
    }
}
