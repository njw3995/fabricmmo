package io.github.njw3995.fabricmmo.core.skill.combat;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Shared mcMMO 2.3.000 combat XP configuration for every combat skill. */
public record CombatXpSettings(
        boolean pvpRewards,
        double pvpMultiplier,
        boolean xpCeilingEnabled,
        double xpDamageLimit,
        double animalsFallback,
        double eggMultiplier,
        double spawnerMultiplier,
        double netherPortalMultiplier,
        double breedingMultiplier,
        double playerTamedMultiplier,
        Map<String, Double> entityMultipliers) {

    public CombatXpSettings {
        if (!Double.isFinite(pvpMultiplier) || pvpMultiplier < 0.0D) {
            throw new IllegalArgumentException("pvpMultiplier must be finite and non-negative");
        }
        if (!Double.isFinite(xpDamageLimit) || xpDamageLimit < 0.0D) {
            throw new IllegalArgumentException("xpDamageLimit must be finite and non-negative");
        }
        if (!Double.isFinite(animalsFallback) || animalsFallback < 0.0D) {
            throw new IllegalArgumentException("animalsFallback must be finite and non-negative");
        }
        validateMultiplier(eggMultiplier, "eggMultiplier");
        validateMultiplier(spawnerMultiplier, "spawnerMultiplier");
        validateMultiplier(netherPortalMultiplier, "netherPortalMultiplier");
        validateMultiplier(breedingMultiplier, "breedingMultiplier");
        validateMultiplier(playerTamedMultiplier, "playerTamedMultiplier");
        entityMultipliers = Map.copyOf(entityMultipliers);
    }

    public static CombatXpSettings load(Path experienceFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(experienceFile);
        HashMap<String, Double> multipliers = new HashMap<>();
        String prefix = "Experience_Values.Combat.Multiplier.";
        for (Map.Entry<String, String> entry : config.valuesWithPrefix(prefix).entrySet()) {
            String key = normalize(entry.getKey().substring(prefix.length()));
            double value = config.requiredDouble(entry.getKey());
            if (!Double.isFinite(value) || value < 0.0D) {
                throw new IllegalArgumentException(
                        "Combat XP multiplier must be finite and non-negative: " + entry.getKey());
            }
            multipliers.put(key, value);
        }
        double animals = multipliers.getOrDefault("animals", 1.0D);
        return new CombatXpSettings(
                config.bool("Experience_Values.PVP.Rewards", true),
                config.decimal("Experience_Formula.Multiplier.PVP", 1.0D),
                config.bool("ExploitFix.Combat.XPCeiling.Enabled", true),
                config.decimal("ExploitFix.Combat.XPCeiling.Damage_Limit", 100.0D),
                animals,
                config.decimal("Experience_Formula.Eggs.Multiplier", 0.0D),
                config.decimal("Experience_Formula.Mobspawners.Multiplier", 0.0D),
                config.decimal("Experience_Formula.Nether_Portal.Multiplier", 0.0D),
                config.decimal("Experience_Formula.Breeding.Multiplier", 1.0D),
                config.decimal("Experience_Formula.Player_Tamed.Multiplier", 0.0D),
                multipliers);
    }

    public double multiplier(String entityPath, boolean animal, boolean monster) {
        String key = upstreamEntityKey(entityPath);
        if (entityMultipliers.containsKey(key)) {
            return entityMultipliers.get(key);
        }
        if (animal) {
            return animalsFallback;
        }
        // Bukkit Monster entities read an absent configuration value as zero.
        return monster ? 0.0D : 1.0D;
    }

    public double pveXp(String entityPath, boolean animal, boolean monster) {
        return multiplier(entityPath, animal, monster) * 10.0D;
    }

    public double originMultiplier(Origin origin) {
        return switch (origin) {
            case NATURAL -> 1.0D;
            case EGG -> eggMultiplier;
            case SPAWNER -> spawnerMultiplier;
            case NETHER_PORTAL -> netherPortalMultiplier;
            case BRED -> breedingMultiplier;
            case PLAYER_TAMED -> playerTamedMultiplier;
            case CALL_OF_THE_WILD -> 0.0D;
        };
    }

    public double pvpXp() {
        return 20.0D * pvpMultiplier;
    }

    public double cappedDamage(double damage) {
        if (!xpCeilingEnabled) {
            return Math.max(0.0D, damage);
        }
        return Math.min(Math.max(0.0D, damage), xpDamageLimit);
    }

    public int awardXp(double baseXp, double actualHealthDamage) {
        double result = Math.max(0.0D, baseXp) * cappedDamage(actualHealthDamage);
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    static String upstreamEntityKey(String entityPath) {
        return switch (normalize(entityPath)) {
            case "mooshroom" -> "mushroom_cow";
            case "snow_golem" -> "snowman";
            default -> normalize(entityPath);
        };
    }

    private static void validateMultiplier(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    public enum Origin {
        NATURAL,
        EGG,
        SPAWNER,
        NETHER_PORTAL,
        BRED,
        PLAYER_TAMED,
        CALL_OF_THE_WILD
    }
}
