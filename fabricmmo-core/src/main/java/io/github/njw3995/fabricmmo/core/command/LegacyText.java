package io.github.njw3995.fabricmmo.core.command;

import java.util.Locale;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

/** Parses upstream ampersand, RGB (&#RRGGBB), and [[FORMATTING_NAME]] locale codes. */
public final class LegacyText {
    private LegacyText() {
    }

    public static Text parse(String input) {
        String value = expandNamedTokens(input == null ? "" : input);
        MutableText root = Text.empty();
        StringBuilder current = new StringBuilder();
        Style active = Style.EMPTY;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '&' || index + 1 >= value.length()) {
                current.append(character);
                continue;
            }
            if (value.charAt(index + 1) == '#' && index + 7 < value.length()) {
                String hex = value.substring(index + 2, index + 8);
                if (hex.matches("[0-9A-Fa-f]{6}")) {
                    append(root, current, active);
                    active = Style.EMPTY.withColor(
                            TextColor.fromRgb(Integer.parseInt(hex, 16)));
                    index += 7;
                    continue;
                }
            }
            Formatting formatting = Formatting.byCode(value.charAt(index + 1));
            if (formatting == null) {
                current.append(character);
                continue;
            }
            append(root, current, active);
            active = formatting == Formatting.RESET
                    ? Style.EMPTY
                    : formatting.isColor()
                            ? Style.EMPTY.withColor(formatting)
                            : active.withFormatting(formatting);
            index++;
        }
        append(root, current, active);
        return root;
    }

    /** Removes all supported upstream formatting codes while preserving visible text. */
    public static String strip(String input) {
        String value = expandNamedTokens(input == null ? "" : input);
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '&' && index + 1 < value.length()) {
                if (value.charAt(index + 1) == '#' && index + 7 < value.length()
                        && value.substring(index + 2, index + 8).matches("[0-9A-Fa-f]{6}")) {
                    index += 7;
                    continue;
                }
                if (Formatting.byCode(value.charAt(index + 1)) != null) {
                    index++;
                    continue;
                }
            }
            result.append(character);
        }
        return result.toString();
    }

    private static String expandNamedTokens(String input) {
        String value = input;
        for (Formatting formatting : Formatting.values()) {
            value = value.replace("[[" + formatting.name() + "]]", "&" + formatting.getCode());
            value = value.replace("[[" + formatting.name().toLowerCase(Locale.ROOT) + "]]",
                    "&" + formatting.getCode());
        }
        return value;
    }

    private static void append(MutableText root, StringBuilder value, Style style) {
        if (value.isEmpty()) {
            return;
        }
        root.append(Text.literal(value.toString()).setStyle(style));
        value.setLength(0);
    }
}
