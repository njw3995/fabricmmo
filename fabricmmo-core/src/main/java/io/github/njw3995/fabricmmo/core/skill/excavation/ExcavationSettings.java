package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.ability.AbilityDurationFormula;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** mcMMO 2.3.000 Excavation ability and Archaeology rank settings. */
public record ExcavationSettings(
        ProgressionMode progressionMode,
        boolean abilitiesEnabled,
        boolean abilityMessages,
        boolean notifyNearbyPlayers,
        boolean onlyActivateWhenSneaking,
        int abilityLengthCapStandard,
        int abilityLengthCapRetro,
        int abilityLengthIncreaseStandard,
        int abilityLengthIncreaseRetro,
        int abilityEnchantBuff,
        int gigaDrillCooldownSeconds,
        int gigaDrillMaximumSeconds,
        int gigaDrillUnlockStandard,
        int gigaDrillUnlockRetro,
        int abilityToolDamage,
        int[] archaeologyStandard,
        int[] archaeologyRetro) {

    public ExcavationSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        if (abilityLengthCapStandard < 0 || abilityLengthCapRetro < 0
                || abilityLengthIncreaseStandard <= 0 || abilityLengthIncreaseRetro <= 0
                || abilityEnchantBuff < 0 || gigaDrillCooldownSeconds < 0
                || gigaDrillMaximumSeconds < 0 || gigaDrillUnlockStandard < 0
                || gigaDrillUnlockRetro < 0 || abilityToolDamage < 0) {
            throw new IllegalArgumentException("Invalid Excavation setting");
        }
        archaeologyStandard = copyRanks(archaeologyStandard);
        archaeologyRetro = copyRanks(archaeologyRetro);
    }

    @Override
    public int[] archaeologyStandard() {
        return archaeologyStandard.clone();
    }

    @Override
    public int[] archaeologyRetro() {
        return archaeologyRetro.clone();
    }

    public static ExcavationSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        boolean retro = config.bool("General.RetroMode.Enabled", true);
        return new ExcavationSettings(
                retro ? ProgressionMode.RETRO : ProgressionMode.STANDARD,
                config.bool("Abilities.Enabled", true),
                config.bool("Abilities.Messages", true),
                advanced.bool(
                        "Feedback.Events.AbilityActivation.SendNotificationToOtherPlayers", true),
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                advanced.integer("Skills.General.Ability.Length.Standard.CapLevel", 100),
                advanced.integer("Skills.General.Ability.Length.RetroMode.CapLevel", 1000),
                advanced.integer("Skills.General.Ability.Length.Standard.IncreaseLevel", 5),
                advanced.integer("Skills.General.Ability.Length.RetroMode.IncreaseLevel", 50),
                advanced.integer("Skills.General.Ability.EnchantBuff", 5),
                config.integer("Abilities.Cooldowns.Giga_Drill_Breaker", 240),
                config.integer("Abilities.Max_Seconds.Giga_Drill_Breaker", 0),
                ranks.integer("Excavation.GigaDrillBreaker.Standard.Rank_1", 5),
                ranks.integer("Excavation.GigaDrillBreaker.RetroMode.Rank_1", 50),
                config.integer("Abilities.Tools.Durability_Loss", 1),
                rankArray(ranks, "Excavation.Archaeology.Standard.Rank_", new int[] {
                        1, 25, 35, 50, 65, 75, 85, 100}),
                rankArray(ranks, "Excavation.Archaeology.RetroMode.Rank_", new int[] {
                        1, 250, 350, 500, 650, 750, 850, 1000}));
    }

    public int gigaDrillUnlockLevel() {
        return progressionMode == ProgressionMode.RETRO
                ? gigaDrillUnlockRetro : gigaDrillUnlockStandard;
    }

    public int gigaDrillDurationSeconds(int level) {
        int cap = progressionMode == ProgressionMode.RETRO
                ? abilityLengthCapRetro : abilityLengthCapStandard;
        int increase = progressionMode == ProgressionMode.RETRO
                ? abilityLengthIncreaseRetro : abilityLengthIncreaseStandard;
        return (int) AbilityDurationFormula.baseDuration(
                level, cap, increase, gigaDrillMaximumSeconds).toSeconds();
    }

    public int archaeologyRank(int level) {
        int[] ranks = progressionMode == ProgressionMode.RETRO
                ? archaeologyRetro : archaeologyStandard;
        int result = 0;
        for (int index = 0; index < ranks.length; index++) {
            if (level >= ranks[index]) {
                result = index + 1;
            }
        }
        return result;
    }

    public double archaeologyOrbChancePercent(int level) {
        return archaeologyRank(level) * 2.0D;
    }

    public int archaeologyOrbAmount(int level) {
        return archaeologyRank(level);
    }

    private static int[] rankArray(FlatYamlConfig config, String prefix, int[] defaults) {
        int[] values = new int[defaults.length];
        for (int index = 0; index < values.length; index++) {
            values[index] = config.integer(prefix + (index + 1), defaults[index]);
        }
        return values;
    }

    private static int[] copyRanks(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length != 8) {
            throw new IllegalArgumentException("Archaeology must have eight ranks");
        }
        int previous = -1;
        int[] copy = values.clone();
        for (int value : copy) {
            if (value < 0 || value < previous) {
                throw new IllegalArgumentException("Archaeology ranks must be non-negative and ordered");
            }
            previous = value;
        }
        return copy;
    }
}
