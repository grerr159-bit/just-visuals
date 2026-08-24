package dev.client.modules.core.render;

import com.google.common.eventbus.Subscribe;
import dev.client.api.nullcry.cmdHelper.managers.dragHandler.DraggableManager;
import dev.client.api.nullcry.events.core.network.PacketEvent;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.other.PushUtils;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.ModeElement;
import dev.client.api.nullcry.modules.settings.Slider;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.uiClient.notification.NotificationDragging;
import dev.client.api.nullcry.uiClient.notification.type.NotificationType;
import dev.client.api.nullcry.uiClient.notification.type.RenderType;
import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interface extends Module {
    public static Interface INSTANCE;

    public Slider blurStrength = new Slider("Сила размытия", () -> true).set(0, 10, 1).defaultValue(3).register(this);
    public ModeElement theme = new ModeElement("Тема", () -> true);

    final String[] Spec = {"Spec", "Спектр", "spec", "спектр", "SPEC", "СПЕКТР", "СПЕКТ"};

    public Interface() {
        super("Interface", ModuleCategory.HUD, "Управляет темой и уведомлениями");

        String[] themes = ThemePalette.displayNames();
        theme.set(themes).applyDefault(ThemePalette.HALLOWEEN.displayName()).register(this);
        for (ThemePalette palette : ThemePalette.values()) {
            theme.withColor(palette.displayName(), palette.previewColor());
        }
    }

    public ThemePalette getSelectedTheme() {
        return ThemePalette.byDisplayName(theme.getValue()).orElse(ThemePalette.HALLOWEEN);
    }

    public int getMainColor() {
        return getSelectedTheme().mainColor();
    }

    public int getMainColor(int index) {
        return ColorUtils.gradient(4, index, getMainColor());
    }

    @Subscribe
    public void onUpdate(UpdateEvent event) {
    }

    @Subscribe
    public void onDraw2D(RenderEvent.Draw2D event) {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            float delta = (float) event.getTickCounter().getLastFrameDuration();
            DraggableManager.renderLingeringPanels(event.getContext(), delta);
        }
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof ChatMessageS2CPacket chatPacket)) return;
        var unsigned = chatPacket.unsignedContent();
        if (unsigned == null) return;

        String originalMessage = unsigned.getString();
        if (originalMessage == null || originalMessage.isEmpty()) return;

        String formattedMessage = Formatting.strip(originalMessage);
        String lowerCaseMessage = formattedMessage.toLowerCase(Locale.ROOT);

        String localPlayer = mc.player != null ? mc.player.getName().getString() : "";
        if (!localPlayer.isEmpty() && formattedMessage.contains(localPlayer)) {
            return;
        }

        if (NotificationDragging.INSTANCE.getElementsNotifications().isSelected("Уведомление о спектре")) {
            for (String spectText : Spec) {
                if (lowerCaseMessage.contains(spectText.toLowerCase(Locale.ROOT))) {
                    String playerName = extractPlayerName(formattedMessage);
                    handlerClient().getNotificationManager().add(
                            "Игрок " + playerName + " начал следить за вами!",
                            2, NotificationType.WARNING, RenderType.WORLD
                    );
                    break;
                }
            }
        }

        if (NotificationDragging.INSTANCE.getElementsNotifications().isSelected("Забанен игрок") && lowerCaseMessage.contains("забанен")) {
            String playerName = bannedPlayerName(formattedMessage);
            String reason = bannedReasonPlayer(unsigned);

            if (!playerName.equalsIgnoreCase(localPlayer) && reason != null) {
                String message = "Игрок " + playerName + " был забанен. " + reason;
                handlerClient().getNotificationManager().add(message, 2, NotificationType.WARNING, RenderType.WORLD);
                if (NotificationDragging.INSTANCE.getNotificationToggle().getEnabled()) {
                    PushUtils.sendPush("Уведомление системы", message);
                }
            }
        }
    }


    private String extractPlayerName(String message) {
        Pattern pattern = Pattern.compile("\\b[a-zA-Z0-9_]{3,16}\\b");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group();
        }
        return "Неизвестный игрок";
    }

    private String bannedPlayerName(String message) {
        Pattern pattern = Pattern.compile("\\b[a-zA-Z0-9_]{3,16}(?=\\s+забанен)");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group();
        }
        return "Неизвестный игрок";
    }

    private String bannedReasonPlayer(Text component) {
        HoverEvent hoverEvent = component.getStyle().getHoverEvent();
        if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Text hoverTextComponent = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
            if (hoverTextComponent != null) {
                String hoverText = hoverTextComponent.getString();

                if (hoverText.contains("[FunAC]")) {
                    return null;
                }

                int reasonIndex = hoverText.indexOf("Причина:");
                if (reasonIndex != -1) {
                    String afterReason = hoverText.substring(reasonIndex + 8).trim();
                    int endIndex = afterReason.indexOf("\n");
                    if (endIndex != -1) {
                        return afterReason.substring(0, endIndex).trim();
                    }
                    return afterReason;
                }
            }
        }
        return "Причина не указана";
    }

    public enum ThemePalette {
        HALLOWEEN("Хэллоуин", new java.awt.Color(255, 105, 0, 255), new java.awt.Color(255, 130, 30, 255)),
        BLOODY_NIGHT("Кровавая ночь", new java.awt.Color(180, 20, 35, 255), new java.awt.Color(210, 35, 45, 255)),
        FROSTBYTE("Морозный", new java.awt.Color(64, 140, 255, 255), new java.awt.Color(90, 160, 255, 255)),
        TOXIC_FOG("Токсичный туман", new java.awt.Color(90, 200, 90, 255), new java.awt.Color(120, 220, 120, 255)),
        VOID_SPIRIT("Дух пустоты", new java.awt.Color(140, 70, 255, 255), new java.awt.Color(165, 95, 255, 255)),
        NEON_RAVE("Неоновый", new java.awt.Color(90, 0, 255, 255), new java.awt.Color(0, 220, 255, 255)),
        GOLDEN_SHINE("Золотой блеск", new java.awt.Color(255, 215, 0, 255), new java.awt.Color(255, 230, 100, 255)),
        AQUAMARINE_DREAM("Аквамариновая мечта", new java.awt.Color(0, 225, 200, 255), new java.awt.Color(0, 255, 170, 255)),
        DARK_MATTER("Темная материя", new java.awt.Color(10, 10, 20, 255), new java.awt.Color(35, 35, 70, 255));

        private final String displayName;
        private final int mainColor;
        private final int previewColor;

        ThemePalette(String displayName, java.awt.Color mainColor, java.awt.Color previewColor) {
            this.displayName = displayName;
            this.mainColor = mainColor.getRGB();
            this.previewColor = previewColor.getRGB();
        }

        public String displayName() {
            return displayName;
        }

        public int mainColor() {
            return mainColor;
        }

        public int previewColor() {
            return previewColor;
        }

        public static String[] displayNames() {
            String[] names = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                names[i] = values()[i].displayName;
            }
            return names;
        }

        public static java.util.Optional<ThemePalette> byDisplayName(String name) {
            if (name == null) {
                return java.util.Optional.empty();
            }
            for (ThemePalette palette : values()) {
                if (palette.displayName.equalsIgnoreCase(name)) {
                    return java.util.Optional.of(palette);
                }
            }
            return java.util.Optional.empty();
        }
    }
}
