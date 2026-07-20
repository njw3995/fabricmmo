package io.github.njw3995.fabricmmo.core.skill.axes;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.ability.AbilityDurationFormula;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Axes mechanics. */
public record AxesSettings(
        ProgressionMode progressionMode,
        boolean abilitiesEnabled,
        boolean abilityMessages,
        boolean notifyNearbyPlayers,
        boolean onlyActivateWhenSneaking,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean adjustForAttackCooldown,
        boolean limitBreakPve,
        NotificationSetting toolReadyNotification,
        NotificationSetting abilityCooldownNotification,
        NotificationSetting superAbilityNotification,
        NotificationSetting abilityOffNotification,
        NotificationSetting abilityRefreshedNotification,
        NotificationSetting superAbilityAlertOthersNotification,
        NotificationSetting subSkillNotification,
        SoundSetting toolReadySound,
        SoundSetting abilityActivatedSound,
        int abilityLengthCapStandard,
        int abilityLengthCapRetro,
        int abilityLengthIncreaseStandard,
        int abilityLengthIncreaseRetro,
        int skullSplitterCooldownSeconds,
        int skullSplitterMaximumSeconds,
        double axeMasteryRankDamageMultiplier,
        double criticalChanceMax,
        int criticalMaximumLevelStandard,
        int criticalMaximumLevelRetro,
        double criticalPvpModifier,
        double criticalPveModifier,
        double greaterImpactChance,
        double greaterImpactKnockbackModifier,
        double greaterImpactBonusDamage,
        double armorImpactChance,
        double armorImpactDamagePerRank,
        double skullSplitterDamageModifier,
        int[] limitBreakUnlocksStandard,
        int[] limitBreakUnlocksRetro,
        int[] skullSplitterUnlocksStandard,
        int[] skullSplitterUnlocksRetro,
        int[] criticalUnlocksStandard,
        int[] criticalUnlocksRetro,
        int[] greaterImpactUnlocksStandard,
        int[] greaterImpactUnlocksRetro,
        int[] armorImpactUnlocksStandard,
        int[] armorImpactUnlocksRetro,
        int[] axeMasteryUnlocksStandard,
        int[] axeMasteryUnlocksRetro,
        boolean greaterImpactParticles) {

    public AxesSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        Objects.requireNonNull(toolReadyNotification, "toolReadyNotification");
        Objects.requireNonNull(abilityCooldownNotification, "abilityCooldownNotification");
        Objects.requireNonNull(superAbilityNotification, "superAbilityNotification");
        Objects.requireNonNull(abilityOffNotification, "abilityOffNotification");
        Objects.requireNonNull(abilityRefreshedNotification, "abilityRefreshedNotification");
        Objects.requireNonNull(superAbilityAlertOthersNotification,
                "superAbilityAlertOthersNotification");
        Objects.requireNonNull(subSkillNotification, "subSkillNotification");
        Objects.requireNonNull(toolReadySound, "toolReadySound");
        Objects.requireNonNull(abilityActivatedSound, "abilityActivatedSound");
        limitBreakUnlocksStandard = copy(limitBreakUnlocksStandard, 10, "limitBreakUnlocksStandard");
        limitBreakUnlocksRetro = copy(limitBreakUnlocksRetro, 10, "limitBreakUnlocksRetro");
        skullSplitterUnlocksStandard = copy(skullSplitterUnlocksStandard, 1, "skullSplitterUnlocksStandard");
        skullSplitterUnlocksRetro = copy(skullSplitterUnlocksRetro, 1, "skullSplitterUnlocksRetro");
        criticalUnlocksStandard = copy(criticalUnlocksStandard, 1, "criticalUnlocksStandard");
        criticalUnlocksRetro = copy(criticalUnlocksRetro, 1, "criticalUnlocksRetro");
        greaterImpactUnlocksStandard = copy(greaterImpactUnlocksStandard, 1, "greaterImpactUnlocksStandard");
        greaterImpactUnlocksRetro = copy(greaterImpactUnlocksRetro, 1, "greaterImpactUnlocksRetro");
        armorImpactUnlocksStandard = copy(armorImpactUnlocksStandard, 20, "armorImpactUnlocksStandard");
        armorImpactUnlocksRetro = copy(armorImpactUnlocksRetro, 20, "armorImpactUnlocksRetro");
        axeMasteryUnlocksStandard = copy(axeMasteryUnlocksStandard, 4, "axeMasteryUnlocksStandard");
        axeMasteryUnlocksRetro = copy(axeMasteryUnlocksRetro, 4, "axeMasteryUnlocksRetro");
        positive(abilityLengthIncreaseStandard, "abilityLengthIncreaseStandard");
        positive(abilityLengthIncreaseRetro, "abilityLengthIncreaseRetro");
        positive(criticalMaximumLevelStandard, "criticalMaximumLevelStandard");
        positive(criticalMaximumLevelRetro, "criticalMaximumLevelRetro");
        positive(criticalPvpModifier, "criticalPvpModifier");
        positive(criticalPveModifier, "criticalPveModifier");
        positive(skullSplitterDamageModifier, "skullSplitterDamageModifier");
        nonNegative(skullSplitterCooldownSeconds, "skullSplitterCooldownSeconds");
        nonNegative(skullSplitterMaximumSeconds, "skullSplitterMaximumSeconds");
        nonNegative(axeMasteryRankDamageMultiplier, "axeMasteryRankDamageMultiplier");
        nonNegative(criticalChanceMax, "criticalChanceMax");
        nonNegative(greaterImpactChance, "greaterImpactChance");
        nonNegative(greaterImpactKnockbackModifier, "greaterImpactKnockbackModifier");
        nonNegative(greaterImpactBonusDamage, "greaterImpactBonusDamage");
        nonNegative(armorImpactChance, "armorImpactChance");
        nonNegative(armorImpactDamagePerRank, "armorImpactDamagePerRank");
    }

    @Override public int[] limitBreakUnlocksStandard() { return limitBreakUnlocksStandard.clone(); }
    @Override public int[] limitBreakUnlocksRetro() { return limitBreakUnlocksRetro.clone(); }
    @Override public int[] skullSplitterUnlocksStandard() { return skullSplitterUnlocksStandard.clone(); }
    @Override public int[] skullSplitterUnlocksRetro() { return skullSplitterUnlocksRetro.clone(); }
    @Override public int[] criticalUnlocksStandard() { return criticalUnlocksStandard.clone(); }
    @Override public int[] criticalUnlocksRetro() { return criticalUnlocksRetro.clone(); }
    @Override public int[] greaterImpactUnlocksStandard() { return greaterImpactUnlocksStandard.clone(); }
    @Override public int[] greaterImpactUnlocksRetro() { return greaterImpactUnlocksRetro.clone(); }
    @Override public int[] armorImpactUnlocksStandard() { return armorImpactUnlocksStandard.clone(); }
    @Override public int[] armorImpactUnlocksRetro() { return armorImpactUnlocksRetro.clone(); }
    @Override public int[] axeMasteryUnlocksStandard() { return axeMasteryUnlocksStandard.clone(); }
    @Override public int[] axeMasteryUnlocksRetro() { return axeMasteryUnlocksRetro.clone(); }

    public static AxesSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile,
            Path soundsFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig sounds = FlatYamlConfig.load(soundsFile);
        double masterVolume = sounds.decimal("Sounds.MasterVolume", 1.0D);
        boolean retro = config.bool("General.RetroMode.Enabled", true);
        return new AxesSettings(
                retro ? ProgressionMode.RETRO : ProgressionMode.STANDARD,
                config.bool("Abilities.Enabled", true),
                config.bool("Abilities.Messages", true),
                advanced.bool("Feedback.Events.AbilityActivation.SendNotificationToOtherPlayers", true),
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                config.bool("Skills.Axes.Enabled_For_PVP", true),
                config.bool("Skills.Axes.Enabled_For_PVE", true),
                advanced.bool("Skills.General.Attack_Cooldown.Adjust_Skills_For_Attack_Cooldown", true),
                advanced.bool("Skills.General.LimitBreak.AllowPVE", false),
                notification(advanced, "ToolReady", true, false),
                notification(advanced, "AbilityCoolDown", true, false),
                notification(advanced, "SuperAbilityInteraction", true, false),
                notification(advanced, "AbilityOff", true, false),
                notification(advanced, "AbilityRefreshed", true, false),
                notification(advanced, "SuperAbilityAlertOthers", true, true),
                notification(advanced, "SubSkillInteraction", true, false),
                sound(sounds, masterVolume, "TOOL_READY",
                        "minecraft:item.armor.equip_gold", 1.0D, 0.4D),
                sound(sounds, masterVolume, "ABILITY_ACTIVATED_GENERIC",
                        "minecraft:item.trident.riptide_3", 1.0D, 0.1D),
                advanced.integer("Skills.General.Ability.Length.Standard.CapLevel", 100),
                advanced.integer("Skills.General.Ability.Length.RetroMode.CapLevel", 1000),
                advanced.integer("Skills.General.Ability.Length.Standard.IncreaseLevel", 5),
                advanced.integer("Skills.General.Ability.Length.RetroMode.IncreaseLevel", 50),
                config.integer("Abilities.Cooldowns.Skull_Splitter", 240),
                config.integer("Abilities.Max_Seconds.Skull_Splitter", 0),
                advanced.decimal("Skills.Axes.AxeMastery.RankDamageMultiplier", 1.0D),
                advanced.decimal("Skills.Axes.CriticalStrikes.ChanceMax", 37.5D),
                advanced.integer("Skills.Axes.CriticalStrikes.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Axes.CriticalStrikes.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Axes.CriticalStrikes.PVP_Modifier", 1.5D),
                advanced.decimal("Skills.Axes.CriticalStrikes.PVE_Modifier", 2.0D),
                advanced.decimal("Skills.Axes.GreaterImpact.Chance", 25.0D),
                advanced.decimal("Skills.Axes.GreaterImpact.KnockbackModifier", 1.5D),
                advanced.decimal("Skills.Axes.GreaterImpact.BonusDamage", 2.0D),
                advanced.decimal("Skills.Axes.ArmorImpact.Chance", 25.0D),
                advanced.decimal("Skills.Axes.ArmorImpact.DamagePerRank", 6.5D),
                advanced.decimal("Skills.Axes.SkullSplitter.DamageModifier", 2.0D),
                rankInts(ranks, "Axes.AxesLimitBreak.Standard.Rank_", 10),
                rankInts(ranks, "Axes.AxesLimitBreak.RetroMode.Rank_", 10),
                rankInts(ranks, "Axes.SkullSplitter.Standard.Rank_", 1),
                rankInts(ranks, "Axes.SkullSplitter.RetroMode.Rank_", 1),
                rankInts(ranks, "Axes.CriticalStrikes.Standard.Rank_", 1),
                rankInts(ranks, "Axes.CriticalStrikes.RetroMode.Rank_", 1),
                rankInts(ranks, "Axes.GreaterImpact.Standard.Rank_", 1),
                rankInts(ranks, "Axes.GreaterImpact.RetroMode.Rank_", 1),
                rankInts(ranks, "Axes.ArmorImpact.Standard.Rank_", 20),
                rankInts(ranks, "Axes.ArmorImpact.RetroMode.Rank_", 20),
                rankInts(ranks, "Axes.AxeMastery.Standard.Rank_", 4),
                rankInts(ranks, "Axes.AxeMastery.RetroMode.Rank_", 4),
                config.bool("Particles.Greater_Impact", true));
    }

    public int limitBreakRank(int level) { return rank(level, limitBreakUnlocks()); }
    public int skullSplitterRank(int level) { return rank(level, skullSplitterUnlocks()); }
    public int criticalRank(int level) { return rank(level, criticalUnlocks()); }
    public int greaterImpactRank(int level) { return rank(level, greaterImpactUnlocks()); }
    public int armorImpactRank(int level) { return rank(level, armorImpactUnlocks()); }
    public int axeMasteryRank(int level) { return rank(level, axeMasteryUnlocks()); }
    public int skullSplitterUnlockLevel() { return skullSplitterUnlocks()[0]; }

    public double criticalChancePercent(int level, boolean lucky) {
        return AxesProbability.criticalChancePercent(
                level, progressionMode, criticalChanceMax,
                criticalMaximumLevelStandard, criticalMaximumLevelRetro, lucky);
    }

    public double staticChancePercent(double chance, boolean lucky) {
        return AxesProbability.staticChancePercent(chance, lucky);
    }

    public double axeMasteryDamage(int level) {
        return AxesDamage.axeMasteryDamage(axeMasteryRank(level), axeMasteryRankDamageMultiplier);
    }

    public double armorImpactRawDamage(int level) {
        return armorImpactRank(level) * armorImpactDamagePerRank;
    }

    public int abilityLengthCap() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthCapRetro : abilityLengthCapStandard;
    }

    public int abilityLengthIncrease() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthIncreaseRetro : abilityLengthIncreaseStandard;
    }

    public int skullSplitterDurationSeconds(int level) {
        Duration duration = AbilityDurationFormula.baseDuration(
                level, abilityLengthCap(), abilityLengthIncrease(), skullSplitterMaximumSeconds);
        return (int) duration.toSeconds();
    }

    public int[] limitBreakUnlocks() { return selected(limitBreakUnlocksStandard, limitBreakUnlocksRetro); }
    public int[] skullSplitterUnlocks() { return selected(skullSplitterUnlocksStandard, skullSplitterUnlocksRetro); }
    public int[] criticalUnlocks() { return selected(criticalUnlocksStandard, criticalUnlocksRetro); }
    public int[] greaterImpactUnlocks() { return selected(greaterImpactUnlocksStandard, greaterImpactUnlocksRetro); }
    public int[] armorImpactUnlocks() { return selected(armorImpactUnlocksStandard, armorImpactUnlocksRetro); }
    public int[] axeMasteryUnlocks() { return selected(axeMasteryUnlocksStandard, axeMasteryUnlocksRetro); }

    public record NotificationSetting(boolean actionBar, boolean copyToChat) { }

    public record SoundSetting(boolean enabled, String id, double volume, double pitch) {
        public SoundSetting {
            Objects.requireNonNull(id, "id");
            nonNegative(volume, "volume");
            nonNegative(pitch, "pitch");
        }
    }

    private static NotificationSetting notification(
            FlatYamlConfig advanced, String key, boolean actionBar, boolean copyToChat) {
        String prefix = "Feedback.ActionBarNotifications." + key + ".";
        return new NotificationSetting(
                advanced.bool(prefix + "Enabled", actionBar),
                advanced.bool(prefix + "SendCopyOfMessageToChat", copyToChat));
    }

    private static SoundSetting sound(
            FlatYamlConfig sounds,
            double masterVolume,
            String key,
            String defaultId,
            double defaultVolume,
            double defaultPitch) {
        String prefix = "Sounds." + key + ".";
        String custom = unquote(sounds.string(prefix + "CustomSoundId", ""));
        return new SoundSetting(
                sounds.bool(prefix + "Enable", true),
                custom.isBlank() ? defaultId : custom,
                masterVolume * sounds.decimal(prefix + "Volume", defaultVolume),
                sounds.decimal(prefix + "Pitch", defaultPitch));
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private int[] selected(int[] standard, int[] retro) {
        return (progressionMode == ProgressionMode.RETRO ? retro : standard).clone();
    }

    private static int rank(int level, int[] unlocks) {
        int result = 0;
        for (int index = 0; index < unlocks.length; index++) {
            if (level >= unlocks[index]) result = index + 1;
        }
        return result;
    }

    private static int[] rankInts(FlatYamlConfig config, String prefix, int count) {
        int[] result = new int[count];
        for (int rank = 1; rank <= count; rank++) {
            result[rank - 1] = config.integer(prefix + rank, 0);
        }
        return result;
    }

    private static int[] copy(int[] values, int length, String name) {
        if (values == null || values.length != length) {
            throw new IllegalArgumentException(name + " must contain " + length + " values");
        }
        return values.clone();
    }

    private static void positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static void nonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
