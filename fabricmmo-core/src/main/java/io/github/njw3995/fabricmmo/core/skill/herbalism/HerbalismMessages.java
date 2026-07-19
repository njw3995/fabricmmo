package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Upstream-compatible English Herbalism ability and passive messages. */
public final class HerbalismMessages {
    private HerbalismMessages() {
    }

    public static Text hoeReady() {
        return Text.literal("You ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal("ready").formatted(Formatting.GOLD))
                .append(Text.literal(" your Hoe.").formatted(Formatting.DARK_AQUA));
    }

    public static Text hoeLowered() {
        return Text.literal("You lower your Hoe.").formatted(Formatting.GRAY);
    }

    public static Text activated() {
        return Text.literal("**GREEN TERRA ACTIVATED**").formatted(Formatting.GREEN);
    }

    public static Text activatedOther(String playerName) {
        return Text.literal(playerName).formatted(Formatting.GREEN)
                .append(Text.literal(" has used ").formatted(Formatting.DARK_GREEN))
                .append(Text.literal("Green Terra!").formatted(Formatting.RED));
    }

    public static Text expired() {
        return Text.literal("**Green Terra has worn off**").formatted(Formatting.RED);
    }

    public static Text expiredOther(String playerName) {
        return Text.literal("Green Terra").formatted(Formatting.WHITE)
                .append(Text.literal(" has worn off for ").formatted(Formatting.GREEN))
                .append(Text.literal(playerName).formatted(Formatting.YELLOW));
    }

    public static Text refreshed() {
        return Text.literal("Your ").formatted(Formatting.GREEN)
                .append(Text.literal("Green Terra ").formatted(Formatting.YELLOW))
                .append(Text.literal("ability is refreshed!").formatted(Formatting.GREEN));
    }

    public static Text cooldown(int seconds) {
        return Text.literal("Green Terra will be ready in " + seconds + " seconds.")
                .formatted(Formatting.RED);
    }

    public static Text locked(int levelsRequired) {
        if (SharedServerSystems.running()) {
            String skillName = SharedServerSystems.require().locale().text("Overhaul.Name.Herbalism");
            return LegacyText.parse(SharedServerSystems.require().locale()
                    .text("Skills.AbilityGateRequirementFail",
                            Integer.toString(levelsRequired), skillName));
        }
        return Text.literal("You require ").formatted(Formatting.GRAY)
                .append(Text.literal(Integer.toString(levelsRequired)).formatted(Formatting.YELLOW))
                .append(Text.literal(" more levels of ").formatted(Formatting.GRAY))
                .append(Text.literal("Herbalism").formatted(Formatting.DARK_AQUA))
                .append(Text.literal(" to use this super ability.").formatted(Formatting.GRAY));
    }

    public static Text greenThumbFailed() {
        return Text.literal("**GREEN THUMB FAIL**").formatted(Formatting.RED);
    }

    public static Text shroomThumbFailed() {
        return Text.literal("**SHROOM THUMB FAIL**").formatted(Formatting.RED);
    }

    public static Text needMore(String itemName) {
        return Text.literal("You need more ").formatted(Formatting.GRAY)
                .append(Text.literal(itemName).formatted(Formatting.YELLOW));
    }
}
