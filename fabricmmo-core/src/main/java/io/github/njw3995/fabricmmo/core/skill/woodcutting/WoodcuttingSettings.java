package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.ability.AbilityDurationFormula;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Complete mcMMO 2.3.000 Woodcutting ability, rank, and exploit settings. */
public record WoodcuttingSettings(
        ProgressionMode progressionMode,
        boolean abilitiesEnabled,
        boolean abilityMessages,
        boolean notifyNearbyPlayers,
        boolean onlyActivateWhenSneaking,
        int abilityLengthCapStandard,
        int abilityLengthCapRetro,
        int abilityLengthIncreaseStandard,
        int abilityLengthIncreaseRetro,
        int treeFellerCooldownSeconds,
        int treeFellerMaximumSeconds,
        int treeFellerUnlockStandard,
        int treeFellerUnlockRetro,
        int treeFellerThreshold,
        int abilityToolDamage,
        boolean treeFellerSounds,
        boolean treeFellerReducedXp,
        boolean knockOnWoodXpOrbs,
        int knockOnWoodRankOneStandard,
        int knockOnWoodRankOneRetro,
        int knockOnWoodRankTwoStandard,
        int knockOnWoodRankTwoRetro,
        int leafBlowerUnlockStandard,
        int leafBlowerUnlockRetro) {

    public WoodcuttingSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        requireNonNegative(abilityLengthCapStandard, "abilityLengthCapStandard");
        requireNonNegative(abilityLengthCapRetro, "abilityLengthCapRetro");
        requirePositive(abilityLengthIncreaseStandard, "abilityLengthIncreaseStandard");
        requirePositive(abilityLengthIncreaseRetro, "abilityLengthIncreaseRetro");
        requireNonNegative(treeFellerCooldownSeconds, "treeFellerCooldownSeconds");
        requireNonNegative(treeFellerMaximumSeconds, "treeFellerMaximumSeconds");
        requireNonNegative(treeFellerUnlockStandard, "treeFellerUnlockStandard");
        requireNonNegative(treeFellerUnlockRetro, "treeFellerUnlockRetro");
        requirePositive(treeFellerThreshold, "treeFellerThreshold");
        requireNonNegative(abilityToolDamage, "abilityToolDamage");
        requireNonNegative(knockOnWoodRankOneStandard, "knockOnWoodRankOneStandard");
        requireNonNegative(knockOnWoodRankOneRetro, "knockOnWoodRankOneRetro");
        requireNonNegative(knockOnWoodRankTwoStandard, "knockOnWoodRankTwoStandard");
        requireNonNegative(knockOnWoodRankTwoRetro, "knockOnWoodRankTwoRetro");
        requireNonNegative(leafBlowerUnlockStandard, "leafBlowerUnlockStandard");
        requireNonNegative(leafBlowerUnlockRetro, "leafBlowerUnlockRetro");
    }

    public static WoodcuttingSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile,
            Path experienceFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        ProgressionMode mode = config.bool("General.RetroMode.Enabled", true)
                ? ProgressionMode.RETRO : ProgressionMode.STANDARD;
        return new WoodcuttingSettings(
                mode,
                config.bool("Abilities.Enabled", true),
                config.bool("Abilities.Messages", true),
                advanced.bool(
                        "Feedback.Events.AbilityActivation.SendNotificationToOtherPlayers", true),
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                advanced.integer("Skills.General.Ability.Length.Standard.CapLevel", 100),
                advanced.integer("Skills.General.Ability.Length.RetroMode.CapLevel", 1000),
                advanced.integer("Skills.General.Ability.Length.Standard.IncreaseLevel", 5),
                advanced.integer("Skills.General.Ability.Length.RetroMode.IncreaseLevel", 50),
                config.integer("Abilities.Cooldowns.Tree_Feller", 240),
                config.integer("Abilities.Max_Seconds.Tree_Feller", 0),
                ranks.integer("Woodcutting.TreeFeller.Standard.Rank_1", 5),
                ranks.integer("Woodcutting.TreeFeller.RetroMode.Rank_1", 50),
                config.integer("Abilities.Limits.Tree_Feller_Threshold", 1000),
                config.integer("Abilities.Tools.Durability_Loss", 1),
                config.bool("Skills.Woodcutting.Tree_Feller_Sounds", true),
                experience.bool("ExploitFix.TreeFellerReducedXP", true),
                advanced.bool(
                        "Skills.Woodcutting.TreeFeller.Knock_On_Wood.Add_XP_Orbs_To_Drops", true),
                ranks.integer("Woodcutting.KnockOnWood.Standard.Rank_1", 30),
                ranks.integer("Woodcutting.KnockOnWood.RetroMode.Rank_1", 300),
                ranks.integer("Woodcutting.KnockOnWood.Standard.Rank_2", 60),
                ranks.integer("Woodcutting.KnockOnWood.RetroMode.Rank_2", 600),
                ranks.integer("Woodcutting.LeafBlower.Standard.Rank_1", 15),
                ranks.integer("Woodcutting.LeafBlower.RetroMode.Rank_1", 150));
    }

    public int treeFellerUnlockLevel() {
        return modeValue(treeFellerUnlockStandard, treeFellerUnlockRetro);
    }

    public int leafBlowerUnlockLevel() {
        return modeValue(leafBlowerUnlockStandard, leafBlowerUnlockRetro);
    }

    public int knockOnWoodRankOneLevel() {
        return modeValue(knockOnWoodRankOneStandard, knockOnWoodRankOneRetro);
    }

    public int knockOnWoodRankTwoLevel() {
        return modeValue(knockOnWoodRankTwoStandard, knockOnWoodRankTwoRetro);
    }

    public int treeFellerDurationSeconds(int skillLevel) {
        return (int) AbilityDurationFormula.baseDuration(
                skillLevel,
                modeValue(abilityLengthCapStandard, abilityLengthCapRetro),
                modeValue(abilityLengthIncreaseStandard, abilityLengthIncreaseRetro),
                treeFellerMaximumSeconds).toSeconds();
    }

    private int modeValue(int standard, int retro) {
        return progressionMode == ProgressionMode.RETRO ? retro : standard;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
