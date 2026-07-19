package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;

/** Deterministic Archaeology treasure and vanilla-XP-orb roll engine. */
public final class ExcavationTreasureRoller {
    private final DoubleSupplier random;

    public ExcavationTreasureRoller(DoubleSupplier random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public Outcome roll(
            NamespacedId blockId,
            int skillLevel,
            boolean gigaDrillActive,
            boolean lucky,
            boolean archaeologyAllowed,
            ExcavationSettings settings,
            ExcavationTreasureTable table) {
        if (!archaeologyAllowed) {
            return Outcome.EMPTY;
        }
        List<ExcavationTreasure> treasures = table.treasures(blockId);
        if (treasures.isEmpty()) {
            return Outcome.EMPTY;
        }
        int rolls = gigaDrillActive ? 3 : 1;
        ArrayList<ExcavationTreasure> treasureDrops = new ArrayList<>();
        ArrayList<Integer> treasureXpAwards = new ArrayList<>();
        ArrayList<Integer> vanillaOrbAwards = new ArrayList<>();
        for (int roll = 0; roll < rolls; roll++) {
            for (ExcavationTreasure treasure : treasures) {
                if (skillLevel < treasure.requiredLevel(settings.progressionMode())
                        || !ExcavationProbability.succeeds(
                                random.getAsDouble(), treasure.dropChancePercent(), lucky)) {
                    continue;
                }
                treasureDrops.add(treasure);
                if (treasure.xp() > 0) {
                    treasureXpAwards.add(treasure.xp());
                }
                if (ExcavationProbability.succeeds(
                        random.getAsDouble(),
                        settings.archaeologyOrbChancePercent(skillLevel),
                        lucky)) {
                    vanillaOrbAwards.add(settings.archaeologyOrbAmount(skillLevel));
                }
            }
        }
        return new Outcome(treasureDrops, treasureXpAwards, vanillaOrbAwards);
    }

    public record Outcome(
            List<ExcavationTreasure> treasureDrops,
            List<Integer> treasureXpAwards,
            List<Integer> vanillaOrbAwards) {
        public static final Outcome EMPTY = new Outcome(List.of(), List.of(), List.of());

        public Outcome {
            treasureDrops = List.copyOf(treasureDrops);
            treasureXpAwards = positiveCopy(treasureXpAwards, "treasure XP");
            vanillaOrbAwards = positiveCopy(vanillaOrbAwards, "vanilla orb XP");
        }

        public int totalTreasureXp() {
            return treasureXpAwards.stream().mapToInt(Integer::intValue).sum();
        }

        public int totalVanillaOrbXp() {
            return vanillaOrbAwards.stream().mapToInt(Integer::intValue).sum();
        }

        private static List<Integer> positiveCopy(List<Integer> values, String description) {
            List<Integer> copy = List.copyOf(values);
            if (copy.stream().anyMatch(value -> value == null || value <= 0)) {
                throw new IllegalArgumentException(description + " awards must be positive");
            }
            return copy;
        }
    }
}
