package io.github.njw3995.fabricmmo.core.skill.mining;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Upstream-compatible English Mining messages used until locale loading is generalized. */
public final class MiningMessages {
    private MiningMessages() {
    }

    public static Text pickaxeReady() {
        return Text.literal("You ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal("ready").formatted(Formatting.GOLD))
                .append(Text.literal(" your pickaxe.").formatted(Formatting.DARK_AQUA));
    }

    public static Text pickaxeLowered() {
        return Text.literal("You lower your Pickaxe.").formatted(Formatting.GRAY);
    }

    public static Text superBreakerActivated() {
        return Text.literal("**SUPER BREAKER ACTIVATED**").formatted(Formatting.GREEN);
    }

    public static Text superBreakerActivatedOther(String playerName) {
        return Text.literal(playerName).formatted(Formatting.GREEN)
                .append(Text.literal(" has used ").formatted(Formatting.DARK_GREEN))
                .append(Text.literal("Super Breaker!").formatted(Formatting.RED));
    }

    public static Text superBreakerExpired() {
        return Text.literal("**Super Breaker has worn off**").formatted(Formatting.RED);
    }

    public static Text superBreakerExpiredOther(String playerName) {
        return Text.literal("Super Breaker ").formatted(Formatting.WHITE)
                .append(Text.literal("has worn off for ").formatted(Formatting.GREEN))
                .append(Text.literal(playerName).formatted(Formatting.YELLOW));
    }

    public static Text superBreakerRefreshed() {
        return Text.literal("Your ").formatted(Formatting.GREEN)
                .append(Text.literal("Super Breaker ").formatted(Formatting.YELLOW))
                .append(Text.literal("ability is refreshed!").formatted(Formatting.GREEN));
    }

    public static Text blastBoom() {
        return Text.literal("**BOOM**").formatted(Formatting.GRAY);
    }

    public static Text blastRefreshed() {
        return Text.literal("Your ").formatted(Formatting.GREEN)
                .append(Text.literal("Blast Mining ").formatted(Formatting.YELLOW))
                .append(Text.literal("ability is refreshed!").formatted(Formatting.GREEN));
    }

    public static Text cooldown(String ability, int seconds) {
        return Text.literal(ability + " will be ready in " + seconds + " seconds.")
                .formatted(Formatting.RED);
    }

    public static Text locked(String ability, int level) {
        return Text.literal("LOCKED UNTIL " + level + "+ SKILL (" + ability + ")")
                .formatted(Formatting.RED);
    }

    public static Text levelUp(int level) {
        return Text.literal("Mining increased to ").formatted(Formatting.WHITE)
                .append(Text.literal(Integer.toString(level)).formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal(".").formatted(Formatting.WHITE));
    }

    public static MutableText header(String title) {
        return Text.literal("-----[] ").formatted(Formatting.GOLD)
                .append(Text.literal(title).formatted(Formatting.GREEN))
                .append(Text.literal(" []-----").formatted(Formatting.GOLD));
    }
}
