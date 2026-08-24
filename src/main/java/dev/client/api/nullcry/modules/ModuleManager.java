package dev.client.api.nullcry.modules;

import com.google.common.eventbus.Subscribe;
import dev.client.Just;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.input.KeyBindEvent;
import dev.client.modules.core.combat.*;
import dev.client.modules.core.misc.*;
import dev.client.modules.core.movement.*;
import dev.client.modules.core.player.*;
import dev.client.modules.core.render.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModuleManager extends CopyOnWriteArrayList<Module> implements ClientApi {

    public void init() {
        registerModule(
                // Combat
                new NoFriendDamage(),

                // Movement
                new Sprint(),

                // Player
                new AutoFix(),
                new AutoRespawn(),
                new AutoArmor(),
                new ItemScroller(),

                // MISCELLANEOUS
                new AutoAuth(),
                new AutoBrewPotion(),
                new AutoClanUpgrade(),
                new AHHelper(),
                new PotionCombiner(),
                new DiscordRPC(),
                new LockSlot(),
                new NameProtect(),

                // Render
                new ArmorDurability(),
                new BlockHighLight(),
                new Crosshair(),
                new AspectRatio(),
                new CustomWorld(),
                new FullBright(),
                new NoRender(),
                new ViewModel(),
                new ChinaHat(),
                new JumpCircles(),
                new Particles(),
                new Interface(),
                new SwordAnimation(),
                new TargetEsp(),
                new HitColor(),
                new Shadow(),
                new SwordHelper(),
                new MaceHelper(),
                new LocationRenderer(),
                new MotionBlur(),
                
                // HUD Modules
                new dev.client.modules.core.hud.WatermarkModule(),
                new dev.client.modules.core.hud.PlayerInfoModule(),
                new dev.client.modules.core.hud.KeybindsModule(),
                new dev.client.modules.core.hud.PotionsModule(),
                new dev.client.modules.core.hud.CooldownsModule(),
                new dev.client.modules.core.hud.TargetHudModule(),
                new dev.client.modules.core.hud.ArmorHudModule(),
                new dev.client.modules.core.hud.InventoryHudModule(),
                new dev.client.modules.core.hud.PartyListModule(),
                new dev.client.modules.core.hud.ScoreboardModule()
        );
        Just.getInstance().getEventBus().register(this);
    }

    private void registerModule(Module... modules) {
        addAll(Arrays.asList(modules));
    }

    public List<Module> getModules() {
        return new java.util.ArrayList<>(this);
    }

    public <T extends Module> T get(final Class<T> clazz) {
        return this.stream()
                .filter(module -> module.getClass().equals(clazz))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    public <T extends Module> T get(final String name, Class<T> clazz) {
        return this.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name) && clazz.isInstance(module))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    public List<Module> get(final ModuleCategory moduleCategory) {
        return this.stream().filter(module -> module.getModuleCategory() == moduleCategory)
                .sorted(java.util.Comparator.comparingInt(this::indexOf))
                .toList();
    }


    @Subscribe
    private void onKey(KeyBindEvent event) {
        for (Module module : this) {
            if (module.getKey() == event.getKey()) {
                module.toggle();
            }
        }
    }
}
