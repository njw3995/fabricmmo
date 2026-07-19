package io.github.njw3995.fabricmmo.core.skill.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Stable English Mining command content modeled after mcMMO 2.3.000. */
public final class MiningCommandFormatter {
    private MiningCommandFormatter() {
    }

    public static List<String> format(MiningCommandSnapshot value) {
        List<String> lines = new ArrayList<>();
        lines.add("Mining Level: " + value.level() + "  XP: " + value.xp() + "/" + value.xpToNextLevel());
        if (value.showBiggerBombs()) {
            lines.add("Bigger Bombs Radius Increase: +" + decimal(value.radiusIncrease()));
        }
        if (value.showBlastMining()) {
            lines.add("Blast Mining: Rank " + value.blastRank() + '/' + value.maximumBlastRank()
                    + " (Ore Bonus " + percent(value.oreBonusPercent())
                    + ", Drop Multiplier " + value.dropMultiplier() + 'x' + ')');
            lines.add(value.blastCooldownSeconds() > 0
                    ? "Blast Mining Cooldown: " + value.blastCooldownSeconds() + "s"
                    : "Blast Mining: Ready");
        }
        if (value.showDemolitionsExpertise()) {
            lines.add("Demolitions Expert Damage Decrease: "
                    + percent(value.demolitionsDamageDecreasePercent()));
        }
        if (value.showDoubleDrops()) {
            lines.add("Double Drops Chance: " + percent(value.doubleDropChancePercent()));
        }
        if (value.showMotherLode()) {
            lines.add("Mother Lode Chance: " + percent(value.motherLodeChancePercent()));
        }
        if (value.showSuperBreaker()) {
            lines.add("Super Breaker Length: " + value.superBreakerDurationSeconds() + "s");
            if (value.superBreakerActive()) {
                lines.add("Super Breaker: ACTIVE (" + value.superBreakerSecondsRemaining() + "s)");
            } else if (value.superBreakerCooldownSeconds() > 0) {
                lines.add("Super Breaker Cooldown: " + value.superBreakerCooldownSeconds() + "s");
            } else {
                lines.add("Super Breaker: Ready");
            }
        }
        return List.copyOf(lines);
    }



    private static String percent(double value) {
        return decimal(value) + '%';
    }

    private static String decimal(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
