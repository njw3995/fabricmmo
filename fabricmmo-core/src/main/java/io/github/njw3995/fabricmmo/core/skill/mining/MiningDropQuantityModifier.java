package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MiningDropQuantityModifier {
    private MiningDropQuantityModifier() {
    }

    public static List<MiningDropStack> apply(
            List<MiningDropStack> drops,
            MiningDropSettings settings,
            MiningDropOutcome outcome) {
        Objects.requireNonNull(drops, "drops");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(outcome, "outcome");
        if (outcome == MiningDropOutcome.NONE || drops.isEmpty()) {
            return List.copyOf(drops);
        }

        DropSafety safety = analyze(drops);
        if (!safety.rewardable()) {
            return List.copyOf(drops);
        }

        List<MiningDropStack> result = new ArrayList<>(drops.size());
        for (MiningDropStack drop : drops) {
            Objects.requireNonNull(drop, "drop");
            if (!settings.materialEnabled(drop.itemId())
                    || (safety.onlyRewardBlocks() && !drop.blockItem())) {
                result.add(drop);
                continue;
            }
            long increased = (long) drop.count() + outcome.bonusItemsToAdd();
            int count = (int) Math.min(increased, drop.maxCount());
            result.add(drop.withCount(count));
        }
        return List.copyOf(result);
    }

    static DropSafety analyze(List<MiningDropStack> drops) {
        Set<NamespacedId> uniqueMaterials = new HashSet<>();
        int blockItems = 0;
        for (MiningDropStack drop : drops) {
            Objects.requireNonNull(drop, "drop");
            uniqueMaterials.add(drop.itemId());
            if (drop.blockItem()) {
                blockItems++;
            }
        }
        return new DropSafety(blockItems <= 1, uniqueMaterials.size() > 1);
    }

    record DropSafety(boolean rewardable, boolean onlyRewardBlocks) {
    }
}
