package dev.client.cmd.core.util;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.function.UnaryOperator;

public final class CommandTextUtil {
    private CommandTextUtil() {
    }

    public static MutableText bracketedButton(String label, Formatting color, ClickEvent.Action clickAction, String clickValue, String hoverText) {
        MutableText button = Text.literal(" [").formatted(Formatting.DARK_GRAY);
        MutableText labelText = Text.literal(label).formatted(color);

        labelText = labelText.styled(applyInteractions(clickAction, clickValue, hoverText));

        button.append(labelText);
        button.append(Text.literal("]").formatted(Formatting.DARK_GRAY));
        return button;
    }

    public static MutableText hintLine(String message, String hoverText, ClickEvent.Action action, String command) {
        MutableText text = Text.literal(message);
        text = text.styled(applyInteractions(action, command, hoverText));
        return text;
    }

    private static UnaryOperator<Style> applyInteractions(ClickEvent.Action action, String value, String hoverText) {
        return style -> {
            Style result = style.withItalic(false);

            if (hoverText != null && !hoverText.isEmpty()) {
                result = result.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverText)));
            }

            if (action != null && value != null && !value.isEmpty()) {
                result = result.withClickEvent(new ClickEvent(action, value));
            }

            return result;
        };
    }
}
