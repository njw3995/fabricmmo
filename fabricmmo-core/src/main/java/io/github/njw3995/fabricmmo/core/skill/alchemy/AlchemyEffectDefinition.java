package io.github.njw3995.fabricmmo.core.skill.alchemy;

import java.util.Locale;
import java.util.Objects;
import net.minecraft.util.Identifier;

public record AlchemyEffectDefinition(Identifier effectId, int amplifier, int duration,
                                      boolean ambient, boolean showParticles, boolean showIcon) {
    public AlchemyEffectDefinition {
        Objects.requireNonNull(effectId, "effectId");
        if (amplifier < 0 || duration < 0) throw new IllegalArgumentException("Invalid potion effect");
    }

    static AlchemyEffectDefinition parse(String raw) {
        String[] parts = raw.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) throw new IllegalArgumentException("Empty potion effect");
        String effect = switch (parts[0].toUpperCase(Locale.ROOT)) {
            case "CONFUSION" -> "nausea";
            case "DAMAGE_RESISTANCE" -> "resistance";
            case "FAST_DIGGING" -> "haste";
            case "SLOW_DIGGING" -> "mining_fatigue";
            default -> parts[0].toLowerCase(Locale.ROOT);
        };
        Identifier id = Identifier.of("minecraft", effect);
        int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int duration = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return new AlchemyEffectDefinition(id, amplifier, duration, false, true, true);
    }
}
