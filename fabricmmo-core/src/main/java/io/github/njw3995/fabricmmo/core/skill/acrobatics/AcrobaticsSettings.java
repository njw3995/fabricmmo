package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Acrobatics settings. */
public record AcrobaticsSettings(
        ProgressionMode progressionMode,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean preventDodgeLightning,
        int teleportXpCooldownSeconds,
        boolean dodgeParticles,
        boolean subSkillMessageActionBar,
        boolean subSkillMessageCopyToChat,
        boolean exploitPrevention,
        boolean dodgeXpFarmingPrevention,
        double dodgeChanceMax,
        int dodgeMaximumLevelStandard,
        int dodgeMaximumLevelRetro,
        double dodgeDamageModifier,
        double rollChanceMax,
        int rollMaximumLevelStandard,
        int rollMaximumLevelRetro,
        double rollDamageThreshold,
        double gracefulRollDamageThreshold,
        double dodgeXpModifier,
        double rollXpModifier,
        double fallXpModifier,
        double featherFallingXpMultiplier,
        int dodgeUnlockStandard,
        int dodgeUnlockRetro,
        boolean rollSoundEnabled,
        String rollSoundId,
        double rollSoundVolume,
        double rollSoundPitch) {

    public AcrobaticsSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        Objects.requireNonNull(rollSoundId, "rollSoundId");
        requireNonNegative(teleportXpCooldownSeconds, "teleportXpCooldownSeconds");
        requirePositive(dodgeMaximumLevelStandard, "dodgeMaximumLevelStandard");
        requirePositive(dodgeMaximumLevelRetro, "dodgeMaximumLevelRetro");
        requirePositive(rollMaximumLevelStandard, "rollMaximumLevelStandard");
        requirePositive(rollMaximumLevelRetro, "rollMaximumLevelRetro");
        requireNonNegative(dodgeUnlockStandard, "dodgeUnlockStandard");
        requireNonNegative(dodgeUnlockRetro, "dodgeUnlockRetro");
        requireNonNegative(dodgeChanceMax, "dodgeChanceMax");
        requirePositive(dodgeDamageModifier, "dodgeDamageModifier");
        requireNonNegative(rollChanceMax, "rollChanceMax");
        requireNonNegative(rollDamageThreshold, "rollDamageThreshold");
        requireNonNegative(gracefulRollDamageThreshold, "gracefulRollDamageThreshold");
        requireNonNegative(dodgeXpModifier, "dodgeXpModifier");
        requireNonNegative(rollXpModifier, "rollXpModifier");
        requireNonNegative(fallXpModifier, "fallXpModifier");
        requireNonNegative(featherFallingXpMultiplier, "featherFallingXpMultiplier");
        requireNonNegative(rollSoundVolume, "rollSoundVolume");
        requireNonNegative(rollSoundPitch, "rollSoundPitch");
    }

    public static AcrobaticsSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile,
            Path experienceFile,
            Path soundsFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        FlatYamlConfig sounds = FlatYamlConfig.load(soundsFile);
        ProgressionMode mode = config.bool("General.RetroMode.Enabled", true)
                ? ProgressionMode.RETRO : ProgressionMode.STANDARD;
        double masterVolume = sounds.decimal("Sounds.MasterVolume", 1.0D);
        String customSound = unquote(sounds.string(
                "Sounds.ROLL_ACTIVATED.CustomSoundId", ""));
        return new AcrobaticsSettings(
                mode,
                config.bool("Skills.Acrobatics.Enabled_For_PVP", true),
                config.bool("Skills.Acrobatics.Enabled_For_PVE", true),
                config.bool("Skills.Acrobatics.Prevent_Dodge_Lightning", false),
                config.integer("Skills.Acrobatics.XP_After_Teleport_Cooldown", 5),
                config.bool("Particles.Dodge", true),
                advanced.bool(
                        "Feedback.ActionBarNotifications.SubSkillInteraction.Enabled", true),
                advanced.bool(
                        "Feedback.ActionBarNotifications.SubSkillInteraction.SendCopyOfMessageToChat",
                        false),
                experience.bool("ExploitFix.Acrobatics", true),
                experience.bool("ExploitFix.AcrobaticsDodgeXpFarming", true),
                advanced.decimal("Skills.Acrobatics.Dodge.ChanceMax", 20.0D),
                advanced.integer("Skills.Acrobatics.Dodge.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Acrobatics.Dodge.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Acrobatics.Dodge.DamageModifier", 2.0D),
                advanced.decimal("Skills.Acrobatics.Roll.ChanceMax", 100.0D),
                advanced.integer("Skills.Acrobatics.Roll.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Acrobatics.Roll.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Acrobatics.Roll.DamageThreshold", 7.0D),
                advanced.decimal("Skills.Acrobatics.GracefulRoll.DamageThreshold", 14.0D),
                experience.decimal("Experience_Values.Acrobatics.Dodge", 800.0D),
                experience.decimal("Experience_Values.Acrobatics.Roll", 600.0D),
                experience.decimal("Experience_Values.Acrobatics.Fall", 600.0D),
                experience.decimal("Experience_Values.Acrobatics.FeatherFall_Multiplier", 2.0D),
                ranks.integer("Acrobatics.Dodge.Standard.Rank_1", 1),
                ranks.integer("Acrobatics.Dodge.RetroMode.Rank_1", 1),
                sounds.bool("Sounds.ROLL_ACTIVATED.Enable", true),
                customSound.isBlank() ? "minecraft:entity.llama.swag" : customSound,
                masterVolume * sounds.decimal("Sounds.ROLL_ACTIVATED.Volume", 1.0D),
                Math.min(2.0D,
                        sounds.decimal("Sounds.ROLL_ACTIVATED.Pitch", 0.7D) + 0.5D));
    }

    public int dodgeUnlockLevel() {
        return progressionMode == ProgressionMode.RETRO
                ? dodgeUnlockRetro : dodgeUnlockStandard;
    }

    public double dodgeChancePercent(int level, boolean lucky) {
        return AcrobaticsProbability.chancePercent(
                level,
                progressionMode,
                dodgeChanceMax,
                dodgeMaximumLevelStandard,
                dodgeMaximumLevelRetro,
                lucky);
    }

    public double rollChancePercent(int level, boolean lucky) {
        return AcrobaticsProbability.chancePercent(
                level,
                progressionMode,
                rollChanceMax,
                rollMaximumLevelStandard,
                rollMaximumLevelRetro,
                lucky);
    }

    public double gracefulRollChancePercent(int level, boolean lucky) {
        return AcrobaticsProbability.gracefulRollChancePercent(
                rollChancePercent(level, lucky));
    }

    /**
     * Pinned mcMMO 2.3.000 rollCheck uses Roll.DamageThreshold * 2 for both normal and graceful
     * successful rolls. This intentionally preserves that observable upstream behavior.
     */
    public double effectiveSuccessfulRollDamageThreshold() {
        return rollDamageThreshold * 2.0D;
    }

    /**
     * Pinned 2.3.000 passes the damaged player into canDodge, so every Dodge check follows the
     * PVP toggle and the PVE/lightning settings are ineffective in that release.
     */
    public boolean pinnedDodgeCombatEnabled() {
        return pvpEnabled;
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("'") && trimmed.endsWith("'"))
                    || (trimmed.startsWith("\"") && trimmed.endsWith("\"")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
