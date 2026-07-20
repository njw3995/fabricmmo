package io.github.njw3995.fabricmmo.core.skill.swords;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.ability.AbilityDurationFormula;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Swords mechanics. */
public record SwordsSettings(
        ProgressionMode progressionMode,
        boolean abilitiesEnabled,
        boolean abilityMessages,
        boolean notifyNearbyPlayers,
        boolean onlyActivateWhenSneaking,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean adjustForAttackCooldown,
        boolean limitBreakPve,
        boolean subSkillMessageActionBar,
        boolean subSkillMessageCopyToChat,
        NotificationSetting toolReadyNotification,
        NotificationSetting abilityCooldownNotification,
        NotificationSetting superAbilityNotification,
        NotificationSetting abilityOffNotification,
        NotificationSetting abilityRefreshedNotification,
        NotificationSetting superAbilityAlertOthersNotification,
        SoundSetting toolReadySound,
        SoundSetting abilityActivatedSound,
        int abilityLengthCapStandard,
        int abilityLengthCapRetro,
        int abilityLengthIncreaseStandard,
        int abilityLengthIncreaseRetro,
        int serratedCooldownSeconds,
        int serratedMaximumSeconds,
        double stabBaseDamage,
        double stabPerRankMultiplier,
        double counterChanceMax,
        int counterMaximumLevelStandard,
        int counterMaximumLevelRetro,
        double counterDamageModifier,
        double serratedDamageModifier,
        int[] counterUnlocksStandard,
        int[] counterUnlocksRetro,
        int[] ruptureUnlocksStandard,
        int[] ruptureUnlocksRetro,
        int[] serratedUnlocksStandard,
        int[] serratedUnlocksRetro,
        int[] stabUnlocksStandard,
        int[] stabUnlocksRetro,
        int[] limitBreakUnlocksStandard,
        int[] limitBreakUnlocksRetro,
        double[] ruptureChance,
        double[] rupturePlayerTickDamage,
        double[] ruptureMobTickDamage,
        int rupturePlayerDurationSeconds,
        int ruptureMobDurationSeconds,
        boolean bleedParticles) {

    public static final int RUPTURE_RANKS = 4;

    public SwordsSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        Objects.requireNonNull(toolReadyNotification, "toolReadyNotification");
        Objects.requireNonNull(abilityCooldownNotification, "abilityCooldownNotification");
        Objects.requireNonNull(superAbilityNotification, "superAbilityNotification");
        Objects.requireNonNull(abilityOffNotification, "abilityOffNotification");
        Objects.requireNonNull(abilityRefreshedNotification, "abilityRefreshedNotification");
        Objects.requireNonNull(superAbilityAlertOthersNotification,
                "superAbilityAlertOthersNotification");
        Objects.requireNonNull(toolReadySound, "toolReadySound");
        Objects.requireNonNull(abilityActivatedSound, "abilityActivatedSound");
        counterUnlocksStandard = copy(counterUnlocksStandard, 1, "counterUnlocksStandard");
        counterUnlocksRetro = copy(counterUnlocksRetro, 1, "counterUnlocksRetro");
        ruptureUnlocksStandard = copy(ruptureUnlocksStandard, 4, "ruptureUnlocksStandard");
        ruptureUnlocksRetro = copy(ruptureUnlocksRetro, 4, "ruptureUnlocksRetro");
        serratedUnlocksStandard = copy(serratedUnlocksStandard, 1, "serratedUnlocksStandard");
        serratedUnlocksRetro = copy(serratedUnlocksRetro, 1, "serratedUnlocksRetro");
        stabUnlocksStandard = copy(stabUnlocksStandard, 2, "stabUnlocksStandard");
        stabUnlocksRetro = copy(stabUnlocksRetro, 2, "stabUnlocksRetro");
        limitBreakUnlocksStandard = copy(limitBreakUnlocksStandard, 10, "limitBreakUnlocksStandard");
        limitBreakUnlocksRetro = copy(limitBreakUnlocksRetro, 10, "limitBreakUnlocksRetro");
        ruptureChance = copy(ruptureChance, 4, "ruptureChance");
        rupturePlayerTickDamage = copy(rupturePlayerTickDamage, 4, "rupturePlayerTickDamage");
        ruptureMobTickDamage = copy(ruptureMobTickDamage, 4, "ruptureMobTickDamage");
        positive(abilityLengthIncreaseStandard, "abilityLengthIncreaseStandard");
        positive(abilityLengthIncreaseRetro, "abilityLengthIncreaseRetro");
        positive(counterMaximumLevelStandard, "counterMaximumLevelStandard");
        positive(counterMaximumLevelRetro, "counterMaximumLevelRetro");
        positive(counterDamageModifier, "counterDamageModifier");
        positive(serratedDamageModifier, "serratedDamageModifier");
        nonNegative(serratedCooldownSeconds, "serratedCooldownSeconds");
        nonNegative(serratedMaximumSeconds, "serratedMaximumSeconds");
        nonNegative(rupturePlayerDurationSeconds, "rupturePlayerDurationSeconds");
        nonNegative(ruptureMobDurationSeconds, "ruptureMobDurationSeconds");
        nonNegative(stabBaseDamage, "stabBaseDamage");
        nonNegative(stabPerRankMultiplier, "stabPerRankMultiplier");
        nonNegative(counterChanceMax, "counterChanceMax");
        validateNonNegative(ruptureChance, "ruptureChance");
        validateNonNegative(rupturePlayerTickDamage, "rupturePlayerTickDamage");
        validateNonNegative(ruptureMobTickDamage, "ruptureMobTickDamage");
    }


    @Override public int[] counterUnlocksStandard() { return counterUnlocksStandard.clone(); }
    @Override public int[] counterUnlocksRetro() { return counterUnlocksRetro.clone(); }
    @Override public int[] ruptureUnlocksStandard() { return ruptureUnlocksStandard.clone(); }
    @Override public int[] ruptureUnlocksRetro() { return ruptureUnlocksRetro.clone(); }
    @Override public int[] serratedUnlocksStandard() { return serratedUnlocksStandard.clone(); }
    @Override public int[] serratedUnlocksRetro() { return serratedUnlocksRetro.clone(); }
    @Override public int[] stabUnlocksStandard() { return stabUnlocksStandard.clone(); }
    @Override public int[] stabUnlocksRetro() { return stabUnlocksRetro.clone(); }
    @Override public int[] limitBreakUnlocksStandard() { return limitBreakUnlocksStandard.clone(); }
    @Override public int[] limitBreakUnlocksRetro() { return limitBreakUnlocksRetro.clone(); }
    @Override public double[] ruptureChance() { return ruptureChance.clone(); }
    @Override public double[] rupturePlayerTickDamage() { return rupturePlayerTickDamage.clone(); }
    @Override public double[] ruptureMobTickDamage() { return ruptureMobTickDamage.clone(); }

    public static SwordsSettings load(
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
        return new SwordsSettings(
                retro ? ProgressionMode.RETRO : ProgressionMode.STANDARD,
                config.bool("Abilities.Enabled", true),
                config.bool("Abilities.Messages", true),
                advanced.bool("Feedback.Events.AbilityActivation.SendNotificationToOtherPlayers", true),
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                config.bool("Skills.Swords.Enabled_For_PVP", true),
                config.bool("Skills.Swords.Enabled_For_PVE", true),
                advanced.bool("Skills.General.Attack_Cooldown.Adjust_Skills_For_Attack_Cooldown", true),
                advanced.bool("Skills.General.LimitBreak.AllowPVE", false),
                advanced.bool("Feedback.ActionBarNotifications.SubSkillInteraction.Enabled", true),
                advanced.bool("Feedback.ActionBarNotifications.SubSkillInteraction.SendCopyOfMessageToChat", false),
                notification(advanced, "ToolReady", true, false),
                notification(advanced, "AbilityCoolDown", true, false),
                notification(advanced, "SuperAbilityInteraction", true, false),
                notification(advanced, "AbilityOff", true, false),
                notification(advanced, "AbilityRefreshed", true, false),
                notification(advanced, "SuperAbilityAlertOthers", true, true),
                sound(sounds, masterVolume, "TOOL_READY",
                        "minecraft:item.armor.equip_gold", 1.0D, 0.4D),
                sound(sounds, masterVolume, "ABILITY_ACTIVATED_GENERIC",
                        "minecraft:item.trident.riptide_3", 1.0D, 0.1D),
                advanced.integer("Skills.General.Ability.Length.Standard.CapLevel", 100),
                advanced.integer("Skills.General.Ability.Length.RetroMode.CapLevel", 1000),
                advanced.integer("Skills.General.Ability.Length.Standard.IncreaseLevel", 5),
                advanced.integer("Skills.General.Ability.Length.RetroMode.IncreaseLevel", 50),
                config.integer("Abilities.Cooldowns.Serrated_Strikes", 240),
                config.integer("Abilities.Max_Seconds.Serrated_Strikes", 0),
                advanced.decimal("Skills.Swords.Stab.Base_Damage", 1.0D),
                advanced.decimal("Skills.Swords.Stab.Per_Rank_Multiplier", 1.5D),
                advanced.decimal("Skills.Swords.CounterAttack.ChanceMax", 30.0D),
                advanced.integer("Skills.Swords.CounterAttack.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Swords.CounterAttack.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Swords.CounterAttack.DamageModifier", 2.0D),
                advanced.decimal("Skills.Swords.SerratedStrikes.DamageModifier", 4.0D),
                rankInts(ranks, "Swords.CounterAttack.Standard.Rank_", 1),
                rankInts(ranks, "Swords.CounterAttack.RetroMode.Rank_", 1),
                rankInts(ranks, "Swords.Rupture.Standard.Rank_", 4),
                rankInts(ranks, "Swords.Rupture.RetroMode.Rank_", 4),
                rankInts(ranks, "Swords.SerratedStrikes.Standard.Rank_", 1),
                rankInts(ranks, "Swords.SerratedStrikes.RetroMode.Rank_", 1),
                rankInts(ranks, "Swords.Stab.Standard.Rank_", 2),
                rankInts(ranks, "Swords.Stab.RetroMode.Rank_", 2),
                rankInts(ranks, "Swords.SwordsLimitBreak.Standard.Rank_", 10),
                rankInts(ranks, "Swords.SwordsLimitBreak.RetroMode.Rank_", 10),
                rankDoubles(advanced,
                        "Skills.Swords.Rupture.Rupture_Mechanics.Chance_To_Apply_On_Hit.Rank_", 4),
                rankDoubles(advanced,
                        "Skills.Swords.Rupture.Rupture_Mechanics.Tick_Interval_Damage.Against_Players.Rank_", 4),
                rankDoubles(advanced,
                        "Skills.Swords.Rupture.Rupture_Mechanics.Tick_Interval_Damage.Against_Mobs.Rank_", 4),
                advanced.integer(
                        "Skills.Swords.Rupture.Rupture_Mechanics.Duration_In_Seconds.Against_Players", 5),
                advanced.integer(
                        "Skills.Swords.Rupture.Rupture_Mechanics.Duration_In_Seconds.Against_Mobs", 5),
                config.bool("Particles.Bleed", true));
    }

    public record NotificationSetting(boolean actionBar, boolean copyToChat) { }

    public record SoundSetting(
            boolean enabled, String id, double volume, double pitch) {
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

    public int counterRank(int level) { return rank(level, counterUnlocks()); }
    public int ruptureRank(int level) { return rank(level, ruptureUnlocks()); }
    public int serratedRank(int level) { return rank(level, serratedUnlocks()); }
    public int stabRank(int level) { return rank(level, stabUnlocks()); }
    public int limitBreakRank(int level) { return rank(level, limitBreakUnlocks()); }
    public int serratedUnlockLevel() { return serratedUnlocks()[0]; }

    public double counterChancePercent(int level, boolean lucky) {
        return SwordsProbability.counterChancePercent(
                level, progressionMode, counterChanceMax,
                counterMaximumLevelStandard, counterMaximumLevelRetro, lucky);
    }

    public double ruptureChancePercent(int rank, boolean lucky) {
        return rank <= 0 ? 0.0D
                : SwordsProbability.ruptureChancePercent(ruptureChance[rank - 1], lucky);
    }

    public double ruptureTickDamage(int rank, boolean playerTarget) {
        if (rank <= 0) return 0.0D;
        return (playerTarget ? rupturePlayerTickDamage : ruptureMobTickDamage)[rank - 1];
    }

    public int ruptureDurationSeconds(boolean playerTarget) {
        return playerTarget ? rupturePlayerDurationSeconds : ruptureMobDurationSeconds;
    }

    public double stabDamage(int level) {
        return SwordsDamage.stabDamage(stabRank(level), stabBaseDamage, stabPerRankMultiplier);
    }

    public int abilityLengthCap() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthCapRetro : abilityLengthCapStandard;
    }

    public int abilityLengthIncrease() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthIncreaseRetro : abilityLengthIncreaseStandard;
    }

    public int serratedDurationSeconds(int level) {
        Duration duration = AbilityDurationFormula.baseDuration(
                level, abilityLengthCap(), abilityLengthIncrease(), serratedMaximumSeconds);
        return (int) duration.toSeconds();
    }

    public int[] counterUnlocks() { return selected(counterUnlocksStandard, counterUnlocksRetro); }
    public int[] ruptureUnlocks() { return selected(ruptureUnlocksStandard, ruptureUnlocksRetro); }
    public int[] serratedUnlocks() { return selected(serratedUnlocksStandard, serratedUnlocksRetro); }
    public int[] stabUnlocks() { return selected(stabUnlocksStandard, stabUnlocksRetro); }
    public int[] limitBreakUnlocks() { return selected(limitBreakUnlocksStandard, limitBreakUnlocksRetro); }

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

    private static double[] rankDoubles(FlatYamlConfig config, String prefix, int count) {
        double[] result = new double[count];
        for (int rank = 1; rank <= count; rank++) {
            result[rank - 1] = config.decimal(prefix + rank, 0.0D);
        }
        return result;
    }

    private static int[] copy(int[] values, int length, String name) {
        if (values == null || values.length != length) {
            throw new IllegalArgumentException(name + " must contain " + length + " values");
        }
        return values.clone();
    }

    private static double[] copy(double[] values, int length, String name) {
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

    private static void validateNonNegative(double[] values, String name) {
        for (double value : values) {
            nonNegative(value, name);
        }
    }
}
