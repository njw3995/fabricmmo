package io.github.njw3995.fabricmmo.core.skill.repair;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Exact mcMMO 2.3.000 Salvage configuration and rank interpretation. */
public record SalvageSettings(
        ProgressionMode progressionMode,
        boolean onlyActivateWhenSneaking,
        boolean anvilMessages,
        boolean anvilPlacedSounds,
        boolean anvilUseSounds,
        String anvilMaterial,
        boolean confirmationRequired,
        boolean allowCustomModelData,
        int[] scrapCollectorStandard,
        int[] scrapCollectorRetro,
        int[] arcaneSalvageStandard,
        int[] arcaneSalvageRetro,
        boolean enchantLossEnabled,
        boolean enchantDowngradeEnabled,
        int maximumEnchantLevel,
        double[] fullExtractionChance,
        double[] partialExtractionChance,
        boolean unsafeEnchantments) {

    public SalvageSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        Objects.requireNonNull(anvilMaterial, "anvilMaterial");
        scrapCollectorStandard = UtilitySkillSettingsSupport.ordered(
                scrapCollectorStandard, "Salvage Scrap Collector Standard ranks");
        scrapCollectorRetro = UtilitySkillSettingsSupport.ordered(
                scrapCollectorRetro, "Salvage Scrap Collector Retro ranks");
        arcaneSalvageStandard = UtilitySkillSettingsSupport.ordered(
                arcaneSalvageStandard, "Salvage Arcane Standard ranks");
        arcaneSalvageRetro = UtilitySkillSettingsSupport.ordered(
                arcaneSalvageRetro, "Salvage Arcane Retro ranks");
        fullExtractionChance = UtilitySkillSettingsSupport.copy(
                fullExtractionChance, 8, "Arcane Salvage full chances");
        partialExtractionChance = UtilitySkillSettingsSupport.copy(
                partialExtractionChance, 8, "Arcane Salvage partial chances");
        if (maximumEnchantLevel <= 0) {
            throw new IllegalArgumentException("Salvage maximum enchantment level must be positive");
        }
    }

    public static SalvageSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile,
            Path experienceFile,
            Path customItemSupportFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        FlatYamlConfig custom = FlatYamlConfig.load(customItemSupportFile);
        ProgressionMode mode = config.bool("General.RetroMode.Enabled", true)
                ? ProgressionMode.RETRO : ProgressionMode.STANDARD;
        return new SalvageSettings(
                mode,
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                config.bool("Skills.Salvage.Anvil_Messages", true),
                config.bool("Skills.Salvage.Anvil_Placed_Sounds", true),
                config.bool("Skills.Salvage.Anvil_Use_Sounds", true),
                config.string("Skills.Salvage.Anvil_Material", "GOLD_BLOCK"),
                config.bool("Skills.Salvage.Confirm_Required", true),
                custom.bool("Custom_Item_Support.Salvage.Allow_Salvage_On_Items_With_Custom_Model_Data", true),
                UtilitySkillSettingsSupport.ranks(ranks,
                        "Salvage.ScrapCollector.Standard.Rank_", 8,
                        new int[] {1, 10, 15, 20, 25, 30, 35, 40}),
                UtilitySkillSettingsSupport.ranks(ranks,
                        "Salvage.ScrapCollector.RetroMode.Rank_", 8,
                        new int[] {1, 100, 150, 200, 250, 300, 350, 400}),
                UtilitySkillSettingsSupport.ranks(ranks,
                        "Salvage.ArcaneSalvage.Standard.Rank_", 8,
                        new int[] {10, 25, 35, 50, 65, 75, 85, 100}),
                UtilitySkillSettingsSupport.ranks(ranks,
                        "Salvage.ArcaneSalvage.RetroMode.Rank_", 8,
                        new int[] {100, 250, 350, 500, 650, 750, 850, 1000}),
                advanced.bool("Skills.Salvage.ArcaneSalvage.EnchantLossEnabled", true),
                advanced.bool("Skills.Salvage.ArcaneSalvage.EnchantDowngradeEnabled", true),
                advanced.integer("Skills.Salvage.ArcaneSalvage.MaxEnchantLevel", 5),
                UtilitySkillSettingsSupport.decimals(advanced,
                        "Skills.Salvage.ArcaneSalvage.ExtractFullEnchant.Rank_", 8,
                        new double[] {2.5, 5, 7.5, 10, 12.5, 17.5, 25, 32.5}),
                UtilitySkillSettingsSupport.decimals(advanced,
                        "Skills.Salvage.ArcaneSalvage.ExtractPartialEnchant.Rank_", 8,
                        new double[] {2, 2.5, 5, 7.5, 10, 12.5, 15, 17.5}),
                experience.bool("ExploitFix.UnsafeEnchantments", false));
    }

    public int scrapCollectorRank(int level) {
        return UtilitySkillSettingsSupport.rank(level,
                UtilitySkillSettingsSupport.mode(
                        progressionMode, scrapCollectorStandard, scrapCollectorRetro));
    }

    public int arcaneSalvageRank(int level) {
        return UtilitySkillSettingsSupport.rank(level,
                UtilitySkillSettingsSupport.mode(
                        progressionMode, arcaneSalvageStandard, arcaneSalvageRetro));
    }

    public double fullExtractionChance(int rank, boolean lucky) {
        return rank <= 0 ? 0.0D : UtilitySkillSettingsSupport.lucky(
                fullExtractionChance[Math.min(rank, fullExtractionChance.length) - 1], lucky);
    }

    public double partialExtractionChance(int rank, boolean lucky) {
        return rank <= 0 ? 0.0D : UtilitySkillSettingsSupport.lucky(
                partialExtractionChance[Math.min(rank, partialExtractionChance.length) - 1], lucky);
    }
}
