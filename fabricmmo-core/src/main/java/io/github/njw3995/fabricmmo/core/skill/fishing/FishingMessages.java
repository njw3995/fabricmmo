package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Upstream-compatible Fishing feedback. */
public final class FishingMessages {
    private FishingMessages() {
    }

    public static Text scared() {
        return locale("Fishing.Scared", Text.literal("Chaotic movements will scare fish!")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
    }

    public static Text lowResources(int moveRange) {
        return locale("Fishing.LowResourcesTip",
                Text.literal("You sense that there might not be many fish left in this area. "
                        + "Try fishing at least " + moveRange + " blocks away.")
                        .formatted(Formatting.GRAY), moveRange);
    }

    public static Text scarcity(int moveRange) {
        return locale("Fishing.ScarcityTip",
                Text.literal("This area is suffering from overfishing, cast your rod in a "
                        + "different spot for more fish. At least " + moveRange + " blocks away.")
                        .formatted(Formatting.YELLOW, Formatting.ITALIC), moveRange);
    }

    public static Text magicFound() {
        return locale("Fishing.Ability.TH.MagicFound",
                Text.literal("You feel a touch of magic with this catch...")
                        .formatted(Formatting.GRAY));
    }

    private static Text locale(String key, Text fallback, Object... values) {
        if (!SharedServerSystems.running()) {
            return fallback;
        }
        return LegacyText.parse(SharedServerSystems.require().locale().text(key, values));
    }
}
