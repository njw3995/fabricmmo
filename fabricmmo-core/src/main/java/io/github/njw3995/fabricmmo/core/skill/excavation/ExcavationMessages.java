package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Upstream-compatible English Excavation ability messages. */
public final class ExcavationMessages {
    private ExcavationMessages() {
    }

    public static Text shovelReady() {
        return Text.literal("You ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal("ready").formatted(Formatting.GOLD))
                .append(Text.literal(" your Shovel.").formatted(Formatting.DARK_AQUA));
    }

    public static Text shovelLowered() {
        return Text.literal("You lower your shovel.").formatted(Formatting.GRAY);
    }

    public static Text activated() {
        return Text.literal("**GIGA DRILL BREAKER ACTIVATED**").formatted(Formatting.GREEN);
    }

    public static Text activatedOther(String playerName) {
        return Text.literal(playerName).formatted(Formatting.GREEN)
                .append(Text.literal(" has used ").formatted(Formatting.DARK_GREEN))
                .append(Text.literal("Giga Drill Breaker!").formatted(Formatting.RED));
    }

    public static Text expired() {
        return Text.literal("**Giga Drill Breaker has worn off**").formatted(Formatting.RED);
    }

    public static Text expiredOther(String playerName) {
        return Text.literal("Giga Drill Breaker").formatted(Formatting.WHITE)
                .append(Text.literal(" has worn off for ").formatted(Formatting.GREEN))
                .append(Text.literal(playerName).formatted(Formatting.YELLOW));
    }

    public static Text refreshed() {
        return Text.literal("Your ").formatted(Formatting.GREEN)
                .append(Text.literal("Giga Drill Breaker ").formatted(Formatting.YELLOW))
                .append(Text.literal("ability is refreshed!").formatted(Formatting.GREEN));
    }

    public static Text cooldown(int seconds) {
        return Text.literal("Giga Drill Breaker will be ready in " + seconds + " seconds.")
                .formatted(Formatting.RED);
    }

    public static Text locked(int levelsRequired) {
        if (SharedServerSystems.running()) {
            String skillName = SharedServerSystems.require().locale()
                    .text("Overhaul.Name.Excavation");
            return LegacyText.parse(SharedServerSystems.require().locale()
                    .text("Skills.AbilityGateRequirementFail",
                            Integer.toString(levelsRequired), skillName));
        }
        return Text.literal("You require ").formatted(Formatting.GRAY)
                .append(Text.literal(Integer.toString(levelsRequired))
                        .formatted(Formatting.YELLOW))
                .append(Text.literal(" more levels of ").formatted(Formatting.GRAY))
                .append(Text.literal("Excavation").formatted(Formatting.DARK_AQUA))
                .append(Text.literal(" to use this super ability.")
                        .formatted(Formatting.GRAY));
    }
}
