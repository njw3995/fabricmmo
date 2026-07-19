package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Woodcutting messages backed by the pinned upstream locale and runtime overrides. */
public final class WoodcuttingMessages {
    private WoodcuttingMessages() {
    }

    public static Text axeReady() {
        return localized("Axes.Ability.Ready",
                Text.literal("You ").formatted(Formatting.DARK_AQUA)
                        .append(Text.literal("ready").formatted(Formatting.GOLD))
                        .append(Text.literal(" your Axe.").formatted(Formatting.DARK_AQUA)));
    }

    public static Text axeLowered() {
        return localized("Axes.Ability.Lower",
                Text.literal("You lower your Axe.").formatted(Formatting.GRAY));
    }

    public static Text axeReadyCooldown(int seconds) {
        if (SharedServerSystems.running()) {
            String abilityName = SharedServerSystems.require().locale()
                    .text("Woodcutting.SubSkill.TreeFeller.Name");
            return LegacyText.parse(SharedServerSystems.require().locale()
                    .text("Axes.Ability.Ready.Extra", abilityName, seconds));
        }
        return axeReady().copy()
                .append(Text.literal(" (Tree Feller is on cooldown for " + seconds + "s)")
                        .formatted(Formatting.GRAY));
    }

    public static Text treeFellerActivated() {
        return localized("Woodcutting.Skills.TreeFeller.On",
                Text.literal("**TREE FELLER ACTIVATED**").formatted(Formatting.GREEN));
    }

    public static Text treeFellerExpired() {
        return localized("Woodcutting.Skills.TreeFeller.Off",
                Text.literal("**Tree Feller has worn off**").formatted(Formatting.GRAY));
    }

    public static Text treeFellerRefreshed() {
        return localized("Woodcutting.Skills.TreeFeller.Refresh",
                Text.literal("Your ").formatted(Formatting.GREEN)
                        .append(Text.literal("Tree Feller").formatted(Formatting.YELLOW))
                        .append(Text.literal(" ability is refreshed!").formatted(Formatting.GREEN)));
    }

    public static Text treeFellerActivatedOther(String name) {
        return localized("Woodcutting.Skills.TreeFeller.Other.On",
                Text.literal(name).formatted(Formatting.GREEN)
                        .append(Text.literal(" has used ").formatted(Formatting.DARK_GREEN))
                        .append(Text.literal("Tree Feller!").formatted(Formatting.RED)),
                name);
    }

    public static Text treeFellerExpiredOther(String name) {
        return localized("Woodcutting.Skills.TreeFeller.Other.Off",
                Text.literal("Tree Feller has worn off for ").formatted(Formatting.GREEN)
                        .append(Text.literal(name).formatted(Formatting.YELLOW)),
                name);
    }

    public static Text locked(int levelsRequired) {
        if (SharedServerSystems.running()) {
            String skillName = SharedServerSystems.require().locale()
                    .text("Overhaul.Name.Woodcutting");
            return LegacyText.parse(SharedServerSystems.require().locale()
                    .text("Skills.AbilityGateRequirementFail", levelsRequired, skillName));
        }
        return Text.literal("You require ").formatted(Formatting.GRAY)
                .append(Text.literal(Integer.toString(levelsRequired)).formatted(Formatting.YELLOW))
                .append(Text.literal(" more levels of ").formatted(Formatting.GRAY))
                .append(Text.literal("Woodcutting").formatted(Formatting.DARK_AQUA))
                .append(Text.literal(" to use this super ability.").formatted(Formatting.GRAY));
    }

    public static Text cooldown(int seconds) {
        return localized("Skills.TooTired",
                Text.literal("You are too tired to use that ability again. ")
                        .append(Text.literal("(" + seconds + "s)").formatted(Formatting.YELLOW)),
                seconds);
    }

    public static Text splinter() {
        return localized("Woodcutting.Skills.TreeFeller.Splinter",
                Text.literal("YOUR AXE SPLINTERS INTO DOZENS OF PIECES!")
                        .formatted(Formatting.RED, Formatting.BOLD));
    }

    public static Text levelUp(int oldLevel, int newLevel) {
        if (SharedServerSystems.running()) {
            String skillName = SharedServerSystems.require().locale()
                    .text("Overhaul.Name.Woodcutting");
            return LegacyText.parse(SharedServerSystems.require().locale()
                    .text("Overhaul.Levelup", skillName, newLevel - oldLevel, newLevel));
        }
        return Text.literal("Woodcutting increased to ").formatted(Formatting.WHITE)
                .append(Text.literal(Integer.toString(newLevel))
                        .formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal(".").formatted(Formatting.WHITE));
    }

    private static Text localized(String key, Text fallback, Object... args) {
        if (SharedServerSystems.running()) {
            return LegacyText.parse(SharedServerSystems.require().locale().text(key, args));
        }
        return fallback;
    }
}
