package io.github.njw3995.fabricmmo.core.combat;

import java.util.Objects;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Pure formatter for the upstream mcMMO mob-healthbar display. */
public final class MobHealthbarFormatter {
    private MobHealthbarFormatter() {
    }

    public static Display display(
            MobHealthbarSettings.DisplayType type,
            double maxHealth,
            double currentHealth) {
        Objects.requireNonNull(type, "type");
        double boundedMax = Math.max(0.0D, maxHealth);
        double boundedCurrent = Math.max(0.0D, currentHealth);
        double percentage = boundedMax == 0.0D
                ? 0.0D
                : (boundedCurrent / boundedMax) * 100.0D;

        int fullDisplay;
        Formatting color;
        String symbol;
        switch (type) {
            case HEARTS -> {
                fullDisplay = Math.min((int) (boundedMax / 2.0D), 10);
                color = Formatting.DARK_RED;
                symbol = "❤";
            }
            case BAR -> {
                fullDisplay = 10;
                color = barColor(percentage);
                symbol = "■";
            }
            case DISABLED -> throw new IllegalArgumentException(
                    "Cannot format a disabled mob healthbar");
            default -> throw new IllegalStateException("Unhandled healthbar type " + type);
        }

        int coloredDisplay = (int) Math.max(
                Math.ceil(fullDisplay * (percentage / 100.0D)), 0.5D);
        int grayDisplay = fullDisplay - coloredDisplay;
        return new Display(symbol, color, coloredDisplay, grayDisplay);
    }

    public static Text text(
            MobHealthbarSettings.DisplayType type,
            double maxHealth,
            double currentHealth) {
        Display display = display(type, maxHealth, currentHealth);
        MutableText result = Text.empty();
        if (display.coloredSymbols() > 0) {
            result.append(Text.literal(display.symbol().repeat(display.coloredSymbols()))
                    .formatted(display.color()));
        }
        if (display.graySymbols() > 0) {
            result.append(Text.literal(display.symbol().repeat(display.graySymbols()))
                    .formatted(Formatting.GRAY));
        }
        return result;
    }

    private static Formatting barColor(double percentage) {
        if (percentage >= 85.0D) {
            return Formatting.DARK_GREEN;
        }
        if (percentage >= 70.0D) {
            return Formatting.GREEN;
        }
        if (percentage >= 55.0D) {
            return Formatting.GOLD;
        }
        if (percentage >= 40.0D) {
            return Formatting.YELLOW;
        }
        if (percentage >= 25.0D) {
            return Formatting.RED;
        }
        return Formatting.DARK_RED;
    }

    public record Display(
            String symbol,
            Formatting color,
            int coloredSymbols,
            int graySymbols) {
        public Display {
            Objects.requireNonNull(symbol, "symbol");
            Objects.requireNonNull(color, "color");
        }
    }
}
