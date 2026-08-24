package dev.client.api.nullcry;

import dev.client.Just;
import dev.client.api.nullcry.render.core.builders.Builder;
import dev.client.api.nullcry.render.core.builders.core.*;
import dev.client.api.nullcry.render.core.msdf.core.MsdfFont;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public interface ClientApi {
    MinecraftClient mc = MinecraftClient.getInstance();
    List<Text> printMessageClient = new ArrayList<>();

    default MutableText gradient(String message, int colorStart) {
        MutableText text = Text.literal("");
        for (int i = 0; i < message.length(); i++) {
            text.append(Text.literal(String.valueOf(message.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorStart))));
        }
        return text;
    }

    default void printClient(String input) {
        if (mc == null || mc.inGameHud == null || mc.inGameHud.getChatHud() == null) return;

        String clientLabel = Just.getInstance().getClientInfo().getName() + " client";
        MutableText prefix = gradient(
                clientLabel,
                Interface.INSTANCE.getMainColor()
        ).copy().setStyle(Style.EMPTY.withBold(true));

        MutableText sep = Text.literal(" -> ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(false));
        MutableText msg = Text.literal(input);
        MutableText finalText = Text.empty()
                .append(prefix)
                .append(sep)
                .append(msg);

        printMessageClient.add(finalText);
        mc.inGameHud.getChatHud().addMessage(finalText);
    }

    default void printClient(Text input) {
        if (mc == null || mc.inGameHud == null || mc.inGameHud.getChatHud() == null) return;

        String clientLabel = Just.getInstance().getClientInfo().getName() + " client";
        MutableText prefix = gradient(
                clientLabel,
                Interface.INSTANCE.getMainColor()
        ).copy().setStyle(Style.EMPTY.withBold(true));

        MutableText sep = Text.literal(" -> ")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(false));

        MutableText finalText = Text.empty()
                .append(prefix)
                .append(sep)
                .append(input.copy());

        printMessageClient.add(finalText);
        mc.inGameHud.getChatHud().addMessage(finalText);
    }

    default void printChat(String message) {
        if (mc == null || mc.player == null) return;

        if (message.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(message.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }

    default void debug(Object msg) {
        String str = msg + "";
        try {
            if (str.contains("PHP_EOL")) {
                String[] msgs = str.split("PHP_EOL");
                for (String message : msgs) {
                    mc.inGameHud.getChatHud().addMessage(Text.literal(message));
                }
                return;
            }
            mc.inGameHud.getChatHud().addMessage(Text.literal(Formatting.AQUA + "[Debug] " + Formatting.RESET + msg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default int calc(int value) {
        Window rs = mc.getWindow();
        return (int) (value * rs.getScaleFactor() / 2);
    }

    static RectangleBuilder rectangle() {
        return Builder.RECTANGLE_BUILDER;
    }

    static GradientBuilder gradient() {
        return Builder.GRADIENT_BUILDER;
    }

    static OutlineBuilder outline() {
        return Builder.BORDER_BUILDER;
    }

    static TextureBuilder drawImage() {
        return Builder.TEXTURE_BUILDER;
    }

    static HeadTextureBuilder headTexture() {
        return Builder.HEAD_TEXTURE_BUILDER;
    }

    static TextBuilder text() {
        return Builder.TEXT_BUILDER;
    }

    static BlurBuilder blur() {
        return Builder.BLUR_BUILDER;
    }

    static ShadowBuilder shadow() {
        return Builder.SHADOW_BUILDER;
    }

    static MsdfFont inter() {
        return Builder.INTER.get();
    }

    static MsdfFont icons() {
        return Builder.ICONS.get();
    }

    static MsdfFont otherIcons() {
        return Builder.OTHER_ICONS.get();
    }
}
