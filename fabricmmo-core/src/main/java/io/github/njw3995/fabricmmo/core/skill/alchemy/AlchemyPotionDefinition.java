package io.github.njw3995.fabricmmo.core.skill.alchemy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.Identifier;

public record AlchemyPotionDefinition(
        String id,
        Identifier materialId,
        String potionType,
        boolean extended,
        boolean upgraded,
        String name,
        Integer color,
        List<String> lore,
        List<AlchemyEffectDefinition> effects,
        Map<Identifier, String> children) {
    public AlchemyPotionDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(materialId, "materialId");
        Objects.requireNonNull(potionType, "potionType");
        lore = List.copyOf(lore);
        effects = List.copyOf(effects);
        children = Map.copyOf(children);
    }

    public boolean water() { return potionType.equalsIgnoreCase("WATER"); }
    public boolean hasEffect() {
        return !effects.isEmpty() || !switch (potionType.toUpperCase(java.util.Locale.ROOT)) {
            case "WATER", "MUNDANE", "THICK", "AWKWARD", "UNCRAFTABLE" -> true;
            default -> false;
        };
    }
    public boolean customAmplifier() {
        return effects.stream().anyMatch(effect -> effect.amplifier() > 0);
    }
    public boolean splashOrLingering() {
        String path = materialId.getPath();
        return path.equals("splash_potion") || path.equals("lingering_potion");
    }
    public AlchemyFormula.PotionShape shape() {
        return new AlchemyFormula.PotionShape(hasEffect(), upgraded, customAmplifier(), extended,
                splashOrLingering());
    }
}
