package dev.client.api.injection;

import dev.client.Just;
import dev.client.api.injection.accessor.IChatHudAccessor;
import dev.client.api.nullcry.ClientApi;
import dev.client.modules.core.misc.NameProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatHudMixins {
    @Unique private static final Pattern COORDS_PATTERN = Pattern.compile("\\[(\\-?\\d+) (\\-?\\d+) (\\-?\\d+)]");

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        Text modified = makeCoordinatesClickable(message);

        if (NameProtect.INSTANCE.isEnabled()) {
            modified = filterComponent(modified);
        }

        ChatHudLine line = new ChatHudLine(
                MinecraftClient.getInstance().inGameHud.getTicks(),
                modified,
                signature,
                indicator
        );

        IChatHudAccessor accessor = (IChatHudAccessor) this;
        accessor.invokeAddVisibleMessage(line);
        accessor.invokeAddMessage(line);
        ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        Style style = ((ChatHud) (Object) this).getTextStyleAt(mouseX, mouseY);
        if (style != null && style.getClickEvent() != null) {
            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
                String value = clickEvent.getValue();

                String prefix = Just.getInstance()
                        .getCmdInitializer()
                        .getCommandDispatcher()
                        .getPrefix()
                        .get();

                if (value.startsWith(prefix + "gps")) {
                    ClientApi iMinecraft = new ClientApi() {};
                    iMinecraft.printChat(value);
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Unique
    private static Text filterComponent(Text component) {
        if (component == null) return Text.empty();

        MutableText copy = component.copy();
        copy.setStyle(component.getStyle());

        if (component.getContent() instanceof PlainTextContent plain) {
            String text = plain.string();
            String nameClient = NameProtect.INSTANCE.nameClient;
            String playerName = MinecraftClient.getInstance().getSession().getUsername();

            text = text.replace(playerName, nameClient);
            if (NameProtect.INSTANCE.friend.getEnabled()) {
                for (String friend : Just.getInstance().getFriendManager().getFriends()) {
                    text = text.replace(friend, nameClient);
                }
            }
            copy = Text.literal(text).setStyle(copy.getStyle());
        }

        for (Text sibling : component.getSiblings()) {
            copy.append(filterComponent(sibling));
        }

        return copy;
    }

    @Unique
    private Text makeCoordinatesClickable(Text component) {
        if (component == null) return null;
        if (component instanceof MutableText) {
            MutableText copy = component.copy();
            copy.getSiblings().replaceAll(this::makeCoordinatesClickable);

            Matcher matcher = COORDS_PATTERN.matcher(copy.getString());
            if (matcher.find()) {
                String x = matcher.group(1);
                String y = matcher.group(2);
                String z = matcher.group(3);

                String prefix = Just.getInstance()
                        .getCmdInitializer()
                        .getCommandDispatcher()
                        .getPrefix()
                        .get();

                String gpsCommand = String.format("%sgps add %s %s %s %s", prefix, "Ивент", x, y, z);

                copy.setStyle(
                        copy.getStyle()
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, gpsCommand))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("§7Нажмите, чтобы вставить GPS-команду")
                                ))
                );
            }

            return copy;
        }
        return component;
    }
}
