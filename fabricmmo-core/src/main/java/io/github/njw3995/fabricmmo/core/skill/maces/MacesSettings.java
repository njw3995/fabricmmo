package io.github.njw3995.fabricmmo.core.skill.maces;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Maces mechanics. */
public record MacesSettings(
        ProgressionMode progressionMode,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean adjustForAttackCooldown,
        boolean limitBreakPve,
        boolean crippleEffectEnabled,
        NotificationSetting subSkillNotification,
        SoundSetting crippleSound,
        double crushBaseDamage,
        double crushRankDamageMultiplier,
        double[] crippleChances,
        int[] limitBreakUnlocksStandard,
        int[] limitBreakUnlocksRetro,
        int[] crippleUnlocksStandard,
        int[] crippleUnlocksRetro,
        int[] crushUnlocksStandard,
        int[] crushUnlocksRetro) {

    public MacesSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        Objects.requireNonNull(subSkillNotification, "subSkillNotification");
        Objects.requireNonNull(crippleSound, "crippleSound");
        crippleChances = copy(crippleChances, 4, "crippleChances");
        limitBreakUnlocksStandard = copy(limitBreakUnlocksStandard, 10,
                "limitBreakUnlocksStandard");
        limitBreakUnlocksRetro = copy(limitBreakUnlocksRetro, 10,
                "limitBreakUnlocksRetro");
        crippleUnlocksStandard = copy(crippleUnlocksStandard, 4,
                "crippleUnlocksStandard");
        crippleUnlocksRetro = copy(crippleUnlocksRetro, 4, "crippleUnlocksRetro");
        crushUnlocksStandard = copy(crushUnlocksStandard, 4, "crushUnlocksStandard");
        crushUnlocksRetro = copy(crushUnlocksRetro, 4, "crushUnlocksRetro");
        nonNegative(crushBaseDamage, "crushBaseDamage");
        nonNegative(crushRankDamageMultiplier, "crushRankDamageMultiplier");
        for (double chance : crippleChances) {
            nonNegative(chance, "crippleChance");
        }
    }

    @Override public double[] crippleChances() { return crippleChances.clone(); }
    @Override public int[] limitBreakUnlocksStandard() { return limitBreakUnlocksStandard.clone(); }
    @Override public int[] limitBreakUnlocksRetro() { return limitBreakUnlocksRetro.clone(); }
    @Override public int[] crippleUnlocksStandard() { return crippleUnlocksStandard.clone(); }
    @Override public int[] crippleUnlocksRetro() { return crippleUnlocksRetro.clone(); }
    @Override public int[] crushUnlocksStandard() { return crushUnlocksStandard.clone(); }
    @Override public int[] crushUnlocksRetro() { return crushUnlocksRetro.clone(); }

    public static MacesSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile,
            Path soundsFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig sounds = FlatYamlConfig.load(soundsFile);
        boolean retro = config.bool("General.RetroMode.Enabled", true);
        double masterVolume = sounds.decimal("Sounds.MasterVolume", 1.0D);
        return new MacesSettings(
                retro ? ProgressionMode.RETRO : ProgressionMode.STANDARD,
                config.bool("Skills.Maces.Enabled_For_PVP", true),
                config.bool("Skills.Maces.Enabled_For_PVE", true),
                advanced.bool(
                        "Skills.General.Attack_Cooldown.Adjust_Skills_For_Attack_Cooldown", true),
                advanced.bool("Skills.General.LimitBreak.AllowPVE", false),
                config.bool("Particles.Cripple", true),
                notification(advanced, "SubSkillInteraction", true, false),
                sound(sounds, masterVolume, "CRIPPLE",
                        "minecraft:item.mace.smash_ground", 1.0D, 0.5D),
                advanced.decimal("Skills.Maces.Crush.Base_Damage", 0.5D),
                advanced.decimal("Skills.Maces.Crush.Rank_Damage_Multiplier", 1.0D),
                new double[] {
                        advanced.decimal(
                                "Skills.Maces.Cripple.Chance_To_Apply_On_Hit.Rank_1", 10.0D),
                        advanced.decimal(
                                "Skills.Maces.Cripple.Chance_To_Apply_On_Hit.Rank_2", 15.0D),
                        advanced.decimal(
                                "Skills.Maces.Cripple.Chance_To_Apply_On_Hit.Rank_3", 20.0D),
                        advanced.decimal(
                                "Skills.Maces.Cripple.Chance_To_Apply_On_Hit.Rank_4", 25.0D)},
                rankInts(ranks, "Maces.MacesLimitBreak.Standard.Rank_", 10),
                rankInts(ranks, "Maces.MacesLimitBreak.RetroMode.Rank_", 10),
                rankInts(ranks, "Maces.Cripple.Standard.Rank_", 4),
                rankInts(ranks, "Maces.Cripple.RetroMode.Rank_", 4),
                rankInts(ranks, "Maces.Crush.Standard.Rank_", 4),
                rankInts(ranks, "Maces.Crush.RetroMode.Rank_", 4));
    }

    public int limitBreakRank(int level) { return rank(level, limitBreakUnlocks()); }
    public int crippleRank(int level) { return rank(level, crippleUnlocks()); }
    public int crushRank(int level) { return rank(level, crushUnlocks()); }

    public double crushDamage(int level) {
        return MacesDamage.crushDamage(
                crushRank(level), crushBaseDamage, crushRankDamageMultiplier);
    }

    public double crippleChancePercent(int level, boolean lucky) {
        int rank = crippleRank(level);
        if (rank <= 0) {
            return 0.0D;
        }
        return MacesProbability.chancePercent(crippleChances[rank - 1], lucky);
    }

    public int[] limitBreakUnlocks() {
        return selected(limitBreakUnlocksStandard, limitBreakUnlocksRetro);
    }
    public int[] crippleUnlocks() { return selected(crippleUnlocksStandard, crippleUnlocksRetro); }
    public int[] crushUnlocks() { return selected(crushUnlocksStandard, crushUnlocksRetro); }

    public static int crippleDurationTicks(boolean playerTarget) {
        return playerTarget ? 20 : 30;
    }

    public static int crippleAmplifier(boolean playerTarget) {
        return playerTarget ? 1 : 2;
    }

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
            if (level >= unlocks[index]) {
                result = index + 1;
            }
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

    private static double[] copy(double[] values, int length, String name) {
        if (values == null || values.length != length) {
            throw new IllegalArgumentException(name + " must contain " + length + " values");
        }
        return values.clone();
    }

    private static void nonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
