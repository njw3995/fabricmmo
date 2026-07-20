package io.github.njw3995.fabricmmo.core.skill.unarmed;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.ability.AbilityDurationFormula;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Unarmed mechanics. */
public record UnarmedSettings(
        ProgressionMode progressionMode,
        boolean abilitiesEnabled,
        boolean abilityMessages,
        boolean notifyNearbyPlayers,
        boolean onlyActivateWhenSneaking,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean adjustForAttackCooldown,
        boolean limitBreakPve,
        boolean itemsAsUnarmed,
        boolean blockCrackerAllowed,
        boolean disarmAntiTheft,
        NotificationSetting toolReadyNotification,
        NotificationSetting abilityCooldownNotification,
        NotificationSetting superAbilityNotification,
        NotificationSetting abilityOffNotification,
        NotificationSetting abilityRefreshedNotification,
        NotificationSetting superAbilityAlertOthersNotification,
        NotificationSetting subSkillNotification,
        SoundSetting toolReadySound,
        SoundSetting abilityActivatedSound,
        SoundSetting glassSound,
        SoundSetting popSound,
        int abilityLengthCapStandard,
        int abilityLengthCapRetro,
        int abilityLengthIncreaseStandard,
        int abilityLengthIncreaseRetro,
        int berserkCooldownSeconds,
        int berserkMaximumSeconds,
        double disarmChanceMax,
        int disarmMaximumLevelStandard,
        int disarmMaximumLevelRetro,
        double arrowDeflectChanceMax,
        int arrowDeflectMaximumLevelStandard,
        int arrowDeflectMaximumLevelRetro,
        double ironGripChanceMax,
        int ironGripMaximumLevelStandard,
        int ironGripMaximumLevelRetro,
        boolean steelArmDamageOverride,
        double[] steelArmOverrides,
        int[] limitBreakUnlocksStandard,
        int[] limitBreakUnlocksRetro,
        int[] berserkUnlocksStandard,
        int[] berserkUnlocksRetro,
        int[] arrowDeflectUnlocksStandard,
        int[] arrowDeflectUnlocksRetro,
        int[] disarmUnlocksStandard,
        int[] disarmUnlocksRetro,
        int[] ironGripUnlocksStandard,
        int[] ironGripUnlocksRetro,
        int[] steelArmUnlocksStandard,
        int[] steelArmUnlocksRetro) {

    public UnarmedSettings {
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
        Objects.requireNonNull(glassSound, "glassSound");
        Objects.requireNonNull(popSound, "popSound");
        steelArmOverrides = copy(steelArmOverrides, 20, "steelArmOverrides");
        limitBreakUnlocksStandard = copy(limitBreakUnlocksStandard, 10, "limitBreakUnlocksStandard");
        limitBreakUnlocksRetro = copy(limitBreakUnlocksRetro, 10, "limitBreakUnlocksRetro");
        berserkUnlocksStandard = copy(berserkUnlocksStandard, 1, "berserkUnlocksStandard");
        berserkUnlocksRetro = copy(berserkUnlocksRetro, 1, "berserkUnlocksRetro");
        arrowDeflectUnlocksStandard = copy(arrowDeflectUnlocksStandard, 1, "arrowDeflectUnlocksStandard");
        arrowDeflectUnlocksRetro = copy(arrowDeflectUnlocksRetro, 1, "arrowDeflectUnlocksRetro");
        disarmUnlocksStandard = copy(disarmUnlocksStandard, 1, "disarmUnlocksStandard");
        disarmUnlocksRetro = copy(disarmUnlocksRetro, 1, "disarmUnlocksRetro");
        ironGripUnlocksStandard = copy(ironGripUnlocksStandard, 1, "ironGripUnlocksStandard");
        ironGripUnlocksRetro = copy(ironGripUnlocksRetro, 1, "ironGripUnlocksRetro");
        steelArmUnlocksStandard = copy(steelArmUnlocksStandard, 20, "steelArmUnlocksStandard");
        steelArmUnlocksRetro = copy(steelArmUnlocksRetro, 20, "steelArmUnlocksRetro");
        positive(abilityLengthIncreaseStandard, "abilityLengthIncreaseStandard");
        positive(abilityLengthIncreaseRetro, "abilityLengthIncreaseRetro");
        positive(disarmMaximumLevelStandard, "disarmMaximumLevelStandard");
        positive(disarmMaximumLevelRetro, "disarmMaximumLevelRetro");
        positive(arrowDeflectMaximumLevelStandard, "arrowDeflectMaximumLevelStandard");
        positive(arrowDeflectMaximumLevelRetro, "arrowDeflectMaximumLevelRetro");
        positive(ironGripMaximumLevelStandard, "ironGripMaximumLevelStandard");
        positive(ironGripMaximumLevelRetro, "ironGripMaximumLevelRetro");
        nonNegative(berserkCooldownSeconds, "berserkCooldownSeconds");
        nonNegative(berserkMaximumSeconds, "berserkMaximumSeconds");
        nonNegative(disarmChanceMax, "disarmChanceMax");
        nonNegative(arrowDeflectChanceMax, "arrowDeflectChanceMax");
        nonNegative(ironGripChanceMax, "ironGripChanceMax");
    }

    @Override public double[] steelArmOverrides() { return steelArmOverrides.clone(); }
    @Override public int[] limitBreakUnlocksStandard() { return limitBreakUnlocksStandard.clone(); }
    @Override public int[] limitBreakUnlocksRetro() { return limitBreakUnlocksRetro.clone(); }
    @Override public int[] berserkUnlocksStandard() { return berserkUnlocksStandard.clone(); }
    @Override public int[] berserkUnlocksRetro() { return berserkUnlocksRetro.clone(); }
    @Override public int[] arrowDeflectUnlocksStandard() { return arrowDeflectUnlocksStandard.clone(); }
    @Override public int[] arrowDeflectUnlocksRetro() { return arrowDeflectUnlocksRetro.clone(); }
    @Override public int[] disarmUnlocksStandard() { return disarmUnlocksStandard.clone(); }
    @Override public int[] disarmUnlocksRetro() { return disarmUnlocksRetro.clone(); }
    @Override public int[] ironGripUnlocksStandard() { return ironGripUnlocksStandard.clone(); }
    @Override public int[] ironGripUnlocksRetro() { return ironGripUnlocksRetro.clone(); }
    @Override public int[] steelArmUnlocksStandard() { return steelArmUnlocksStandard.clone(); }
    @Override public int[] steelArmUnlocksRetro() { return steelArmUnlocksRetro.clone(); }

    public static UnarmedSettings load(
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
        return new UnarmedSettings(
                retro ? ProgressionMode.RETRO : ProgressionMode.STANDARD,
                config.bool("Abilities.Enabled", true),
                config.bool("Abilities.Messages", true),
                advanced.bool("Feedback.Events.AbilityActivation.SendNotificationToOtherPlayers", true),
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                config.bool("Skills.Unarmed.Enabled_For_PVP", true),
                config.bool("Skills.Unarmed.Enabled_For_PVE", true),
                advanced.bool("Skills.General.Attack_Cooldown.Adjust_Skills_For_Attack_Cooldown", true),
                advanced.bool("Skills.General.LimitBreak.AllowPVE", false),
                config.bool("Skills.Unarmed.Items_As_Unarmed", false),
                config.bool("Skills.Unarmed.Block_Cracker.Allow_Block_Cracker", true),
                advanced.bool("Skills.Unarmed.Disarm.AntiTheft", false),
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
                sound(sounds, masterVolume, "GLASS",
                        "minecraft:block.glass.break", 1.0D, 1.0D),
                sound(sounds, masterVolume, "POP",
                        "minecraft:entity.item.pickup", 0.2D, 1.0D),
                advanced.integer("Skills.General.Ability.Length.Standard.CapLevel", 100),
                advanced.integer("Skills.General.Ability.Length.RetroMode.CapLevel", 1000),
                advanced.integer("Skills.General.Ability.Length.Standard.IncreaseLevel", 5),
                advanced.integer("Skills.General.Ability.Length.RetroMode.IncreaseLevel", 50),
                config.integer("Abilities.Cooldowns.Berserk", 240),
                config.integer("Abilities.Max_Seconds.Berserk", 0),
                advanced.decimal("Skills.Unarmed.Disarm.ChanceMax", 33.0D),
                advanced.integer("Skills.Unarmed.Disarm.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Unarmed.Disarm.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Unarmed.ArrowDeflect.ChanceMax", 50.0D),
                advanced.integer("Skills.Unarmed.ArrowDeflect.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Unarmed.ArrowDeflect.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Unarmed.IronGrip.ChanceMax", 100.0D),
                advanced.integer("Skills.Unarmed.IronGrip.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Unarmed.IronGrip.MaxBonusLevel.RetroMode", 1000),
                advanced.bool("Skills.Unarmed.SteelArmStyle.Damage_Override", false),
                rankDoubles(advanced, "Skills.Unarmed.SteelArmStyle.Override.Rank_", 20),
                rankInts(ranks, "Unarmed.UnarmedLimitBreak.Standard.Rank_", 10),
                rankInts(ranks, "Unarmed.UnarmedLimitBreak.RetroMode.Rank_", 10),
                rankInts(ranks, "Unarmed.Berserk.Standard.Rank_", 1),
                rankInts(ranks, "Unarmed.Berserk.RetroMode.Rank_", 1),
                rankInts(ranks, "Unarmed.ArrowDeflect.Standard.Rank_", 1),
                rankInts(ranks, "Unarmed.ArrowDeflect.RetroMode.Rank_", 1),
                rankInts(ranks, "Unarmed.Disarm.Standard.Rank_", 1),
                rankInts(ranks, "Unarmed.Disarm.RetroMode.Rank_", 1),
                rankInts(ranks, "Unarmed.IronGrip.Standard.Rank_", 1),
                rankInts(ranks, "Unarmed.IronGrip.RetroMode.Rank_", 1),
                rankInts(ranks, "Unarmed.SteelArmStyle.Standard.Rank_", 20),
                rankInts(ranks, "Unarmed.SteelArmStyle.RetroMode.Rank_", 20));
    }

    public int limitBreakRank(int level) { return rank(level, limitBreakUnlocks()); }
    public int berserkRank(int level) { return rank(level, berserkUnlocks()); }
    public int arrowDeflectRank(int level) { return rank(level, arrowDeflectUnlocks()); }
    public int disarmRank(int level) { return rank(level, disarmUnlocks()); }
    public int ironGripRank(int level) { return rank(level, ironGripUnlocks()); }
    public int steelArmRank(int level) { return rank(level, steelArmUnlocks()); }
    public int berserkUnlockLevel() { return berserkUnlocks()[0]; }

    public double steelArmDamage(int level) {
        return UnarmedDamage.steelArmDamage(
                steelArmRank(level), steelArmDamageOverride, steelArmOverrides);
    }

    public double disarmChancePercent(int level, boolean lucky) {
        return chance(level, disarmChanceMax, disarmMaximumLevelStandard,
                disarmMaximumLevelRetro, lucky);
    }

    public double arrowDeflectChancePercent(int level, boolean lucky) {
        return chance(level, arrowDeflectChanceMax, arrowDeflectMaximumLevelStandard,
                arrowDeflectMaximumLevelRetro, lucky);
    }

    public double ironGripChancePercent(int level, boolean lucky) {
        return chance(level, ironGripChanceMax, ironGripMaximumLevelStandard,
                ironGripMaximumLevelRetro, lucky);
    }

    public int abilityLengthCap() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthCapRetro : abilityLengthCapStandard;
    }

    public int abilityLengthIncrease() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthIncreaseRetro : abilityLengthIncreaseStandard;
    }

    public int berserkDurationSeconds(int level) {
        Duration duration = AbilityDurationFormula.baseDuration(
                level, abilityLengthCap(), abilityLengthIncrease(), berserkMaximumSeconds);
        return (int) duration.toSeconds();
    }

    public int[] limitBreakUnlocks() { return selected(limitBreakUnlocksStandard, limitBreakUnlocksRetro); }
    public int[] berserkUnlocks() { return selected(berserkUnlocksStandard, berserkUnlocksRetro); }
    public int[] arrowDeflectUnlocks() { return selected(arrowDeflectUnlocksStandard, arrowDeflectUnlocksRetro); }
    public int[] disarmUnlocks() { return selected(disarmUnlocksStandard, disarmUnlocksRetro); }
    public int[] ironGripUnlocks() { return selected(ironGripUnlocksStandard, ironGripUnlocksRetro); }
    public int[] steelArmUnlocks() { return selected(steelArmUnlocksStandard, steelArmUnlocksRetro); }

    public record NotificationSetting(boolean actionBar, boolean copyToChat) { }

    public record SoundSetting(boolean enabled, String id, double volume, double pitch) {
        public SoundSetting {
            Objects.requireNonNull(id, "id");
            nonNegative(volume, "volume");
            nonNegative(pitch, "pitch");
        }
    }

    private double chance(
            int level, double maximum, int standardCap, int retroCap, boolean lucky) {
        return UnarmedProbability.chancePercent(
                level, progressionMode, maximum, standardCap, retroCap, lucky);
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

    private static double[] rankDoubles(FlatYamlConfig config, String prefix, int count) {
        double[] result = new double[count];
        for (int rank = 1; rank <= count; rank++) {
            result[rank - 1] = config.decimal(prefix + rank,
                    UnarmedDamage.steelArmDamage(rank, false, new double[0]));
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
}
