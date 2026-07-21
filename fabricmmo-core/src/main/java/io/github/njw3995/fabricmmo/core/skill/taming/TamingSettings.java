package io.github.njw3995.fabricmmo.core.skill.taming;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;
import net.minecraft.util.Identifier;

/** Config-backed mcMMO 2.3.000 Taming settings. */
public record TamingSettings(
        ProgressionMode progressionMode,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean cotwBreedingPrevented,
        double goreModifier,
        double fastFoodChance,
        double thickFurModifier,
        double shockProofModifier,
        double sharpenedClawsBonus,
        double pummelChance,
        double minHorseJumpStrength,
        double maxHorseJumpStrength,
        int beastLoreUnlock,
        int goreUnlock,
        int callOfTheWildUnlock,
        int pummelUnlock,
        int fastFoodUnlock,
        int environmentallyAwareUnlock,
        int thickFurUnlock,
        int holyHoundUnlock,
        int shockProofUnlock,
        int sharpenedClawsUnlock,
        Map<TamingSummonType, TamingSummonSettings> summons) {

    public TamingSettings { summons = Map.copyOf(summons); }

    public static TamingSettings load(Path configFile, Path advancedFile, Path skillRanksFile,
                                      Path experienceFile,
                                      ProgressionMode mode) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        String rankMode = mode == ProgressionMode.RETRO ? "RetroMode" : "Standard";
        EnumMap<TamingSummonType, TamingSummonSettings> summons =
                new EnumMap<>(TamingSummonType.class);
        for (TamingSummonType type : TamingSummonType.values()) {
            String base = "Skills.Taming.Call_Of_The_Wild." + type.configName() + ".";
            String material = config.string(base + "Item_Material",
                    type.defaultItemId().toString());
            Identifier itemId = parseItemId(material);
            summons.put(type, new TamingSummonSettings(
                    itemId,
                    config.integer(base + "Item_Amount", 10),
                    config.integer(base + "Summon_Amount", 1),
                    config.integer(base + "Summon_Length", 240),
                    config.integer(base + "Per_Player_Limit", type == TamingSummonType.WOLF ? 2 : 1)));
        }
        return new TamingSettings(
                mode,
                config.bool("Skills.Taming.Enabled_For_PVP", true),
                config.bool("Skills.Taming.Enabled_For_PVE", true),
                experience.bool("ExploitFix.COTWBreeding", true),
                advanced.decimal("Skills.Taming.Gore.Modifier", 2.0D),
                advanced.decimal("Skills.Taming.FastFoodService.Chance", 50.0D),
                advanced.decimal("Skills.Taming.ThickFur.Modifier", 2.0D),
                advanced.decimal("Skills.Taming.ShockProof.Modifier", 6.0D),
                advanced.decimal("Skills.Taming.SharpenedClaws.Bonus", 2.0D),
                advanced.decimal("Skills.Taming.Pummel.Chance", 10.0D),
                advanced.decimal("Skills.Taming.CallOfTheWild.MinHorseJumpStrength", 0.7D),
                advanced.decimal("Skills.Taming.CallOfTheWild.MaxHorseJumpStrength", 2.0D),
                unlock(ranks, "BeastLore", rankMode, 1),
                unlock(ranks, "Gore", rankMode, mode == ProgressionMode.RETRO ? 150 : 15),
                unlock(ranks, "CallOfTheWild", rankMode, 1),
                unlock(ranks, "Pummel", rankMode, mode == ProgressionMode.RETRO ? 200 : 20),
                unlock(ranks, "FastFoodService", rankMode, mode == ProgressionMode.RETRO ? 200 : 20),
                unlock(ranks, "EnvironmentallyAware", rankMode, mode == ProgressionMode.RETRO ? 100 : 10),
                unlock(ranks, "ThickFur", rankMode, mode == ProgressionMode.RETRO ? 250 : 25),
                unlock(ranks, "HolyHound", rankMode, mode == ProgressionMode.RETRO ? 350 : 35),
                unlock(ranks, "ShockProof", rankMode, mode == ProgressionMode.RETRO ? 500 : 50),
                unlock(ranks, "SharpenedClaws", rankMode, mode == ProgressionMode.RETRO ? 750 : 75),
                summons);
    }

    private static Identifier parseItemId(String configuredValue) {
        String material = configuredValue.trim();
        if (material.isEmpty()) {
            throw new IllegalArgumentException("Call of the Wild item ID must not be blank");
        }
        String normalized = material.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        Identifier itemId = Identifier.tryParse(normalized);
        if (itemId == null) {
            throw new IllegalArgumentException(
                    "Invalid Call of the Wild item ID: " + configuredValue);
        }
        return itemId;
    }

    private static int unlock(FlatYamlConfig ranks, String skill, String mode, int fallback) {
        return ranks.integer("Taming." + skill + "." + mode + ".Rank_1", fallback);
    }
}
