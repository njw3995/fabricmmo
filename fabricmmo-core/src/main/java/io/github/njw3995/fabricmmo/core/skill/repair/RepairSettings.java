package io.github.njw3995.fabricmmo.core.skill.repair;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Exact mcMMO 2.3.000 Repair configuration and rank interpretation. */
public record RepairSettings(
        ProgressionMode progressionMode,
        boolean onlyActivateWhenSneaking,
        boolean anvilMessages,
        boolean anvilPlacedSounds,
        boolean anvilUseSounds,
        String anvilMaterial,
        boolean useEnchantedMaterials,
        boolean confirmationRequired,
        boolean allowCustomModelData,
        boolean repairMasteryEnabled,
        double masteryMaximumBonusPercentage,
        int masteryMaximumBonusStandard,
        int masteryMaximumBonusRetro,
        int repairMasteryStandard,
        int repairMasteryRetro,
        double superRepairChanceMaximum,
        int superRepairMaximumBonusStandard,
        int superRepairMaximumBonusRetro,
        int superRepairStandard,
        int superRepairRetro,
        int[] arcaneForgingStandard,
        int[] arcaneForgingRetro,
        boolean arcaneMayLoseEnchants,
        int arcaneMaximumEnchantLevel,
        double[] keepEnchantChance,
        boolean arcaneDowngradesEnabled,
        double[] avoidDowngradeChance,
        boolean unsafeEnchantments,
        double baseXp,
        Map<UtilityMaterialCategory, Double> materialXpMultipliers) {

    public RepairSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        Objects.requireNonNull(anvilMaterial, "anvilMaterial");
        arcaneForgingStandard = UtilitySkillSettingsSupport.ordered(
                arcaneForgingStandard, "Repair Arcane Forging Standard ranks");
        arcaneForgingRetro = UtilitySkillSettingsSupport.ordered(
                arcaneForgingRetro, "Repair Arcane Forging Retro ranks");
        keepEnchantChance = UtilitySkillSettingsSupport.copy(
                keepEnchantChance, 8, "Repair Arcane keep chances");
        avoidDowngradeChance = UtilitySkillSettingsSupport.copy(
                avoidDowngradeChance, 8, "Repair Arcane avoid-downgrade chances");
        materialXpMultipliers = Map.copyOf(materialXpMultipliers);
        if (masteryMaximumBonusStandard <= 0 || masteryMaximumBonusRetro <= 0
                || superRepairMaximumBonusStandard <= 0 || superRepairMaximumBonusRetro <= 0
                || masteryMaximumBonusPercentage < 0.0D || superRepairChanceMaximum < 0.0D
                || arcaneMaximumEnchantLevel <= 0 || baseXp < 0.0D) {
            throw new IllegalArgumentException("Invalid Repair settings");
        }
    }

    public static RepairSettings load(
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
        EnumMap<UtilityMaterialCategory, Double> xp = new EnumMap<>(UtilityMaterialCategory.class);
        for (UtilityMaterialCategory category : UtilityMaterialCategory.values()) {
            xp.put(category, experience.decimal(
                    "Experience_Values.Repair." + category.experienceKey(), 1.0D));
        }
        return new RepairSettings(
                mode,
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                config.bool("Skills.Repair.Anvil_Messages", true),
                config.bool("Skills.Repair.Anvil_Placed_Sounds", true),
                config.bool("Skills.Repair.Anvil_Use_Sounds", true),
                config.string("Skills.Repair.Anvil_Material", "IRON_BLOCK"),
                advanced.bool("Skills.Repair.Use_Enchanted_Materials", false),
                config.bool("Skills.Repair.Confirm_Required", true),
                custom.bool("Custom_Item_Support.Repair.Allow_Repair_On_Items_With_Custom_Model_Data", true),
                true,
                advanced.decimal("Skills.Repair.RepairMastery.MaxBonusPercentage", 200.0D),
                advanced.integer("Skills.Repair.RepairMastery.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Repair.RepairMastery.MaxBonusLevel.RetroMode", 1000),
                ranks.integer("Repair.RepairMastery.Standard.Rank_1", 1),
                ranks.integer("Repair.RepairMastery.RetroMode.Rank_1", 1),
                advanced.decimal("Skills.Repair.SuperRepair.ChanceMax", 100.0D),
                advanced.integer("Skills.Repair.SuperRepair.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Repair.SuperRepair.MaxBonusLevel.RetroMode", 1000),
                ranks.integer("Repair.SuperRepair.Standard.Rank_1", 40),
                ranks.integer("Repair.SuperRepair.RetroMode.Rank_1", 400),
                UtilitySkillSettingsSupport.ranks(ranks,
                        "Repair.ArcaneForging.Standard.Rank_", 8,
                        new int[] {10, 25, 35, 50, 65, 75, 85, 100}),
                UtilitySkillSettingsSupport.ranks(ranks,
                        "Repair.ArcaneForging.RetroMode.Rank_", 8,
                        new int[] {100, 250, 350, 500, 650, 750, 850, 1000}),
                advanced.bool("Skills.Repair.ArcaneForging.May_Lose_Enchants", true),
                advanced.integer("Skills.Repair.ArcaneForging.MaxEnchantLevel", 5),
                UtilitySkillSettingsSupport.decimals(advanced,
                        "Skills.Repair.ArcaneForging.Keep_Enchants_Chance.Rank_", 8,
                        new double[] {10, 20, 30, 40, 50, 50, 60, 60}),
                advanced.bool("Skills.Repair.ArcaneForging.Downgrades_Enabled", true),
                UtilitySkillSettingsSupport.decimals(advanced,
                        "Skills.Repair.ArcaneForging.Downgrades_Chance.Rank_", 8,
                        new double[] {75, 50, 40, 30, 25, 20, 15, 10}),
                experience.bool("ExploitFix.UnsafeEnchantments", false),
                experience.decimal("Experience_Values.Repair.Base", 1000.0D),
                xp);
    }

    public boolean repairMasteryUnlocked(int level) {
        return level >= UtilitySkillSettingsSupport.mode(
                progressionMode, repairMasteryStandard, repairMasteryRetro);
    }

    public boolean superRepairUnlocked(int level) {
        return level >= UtilitySkillSettingsSupport.mode(
                progressionMode, superRepairStandard, superRepairRetro);
    }

    public int arcaneForgingRank(int level) {
        return UtilitySkillSettingsSupport.rank(level,
                UtilitySkillSettingsSupport.mode(
                        progressionMode, arcaneForgingStandard, arcaneForgingRetro));
    }

    public int masteryMaximumBonusLevel() {
        return UtilitySkillSettingsSupport.mode(
                progressionMode, masteryMaximumBonusStandard, masteryMaximumBonusRetro);
    }

    public double superRepairChance(int level, boolean lucky) {
        if (!superRepairUnlocked(level)) {
            return 0.0D;
        }
        int maximum = UtilitySkillSettingsSupport.mode(
                progressionMode, superRepairMaximumBonusStandard, superRepairMaximumBonusRetro);
        double base = Math.min(superRepairChanceMaximum,
                superRepairChanceMaximum * Math.min(level, maximum) / maximum);
        return UtilitySkillSettingsSupport.lucky(base, lucky);
    }

    public double keepEnchantChance(int rank, boolean lucky) {
        return rank <= 0 ? 0.0D : UtilitySkillSettingsSupport.lucky(
                keepEnchantChance[Math.min(rank, keepEnchantChance.length) - 1], lucky);
    }

    /** Upstream table is the chance to downgrade, so this returns the chance to avoid it. */
    public double avoidDowngradeChance(int rank, boolean lucky) {
        if (rank <= 0) {
            return 0.0D;
        }
        double downgrade = avoidDowngradeChance[Math.min(rank, avoidDowngradeChance.length) - 1];
        return UtilitySkillSettingsSupport.lucky(100.0D - downgrade, lucky);
    }

    public double materialXpMultiplier(UtilityMaterialCategory category) {
        return materialXpMultipliers.getOrDefault(category, 1.0D);
    }
}
