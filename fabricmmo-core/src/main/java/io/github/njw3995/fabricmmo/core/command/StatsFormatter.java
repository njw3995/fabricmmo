package io.github.njw3995.fabricmmo.core.command;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StatsFormatter {
    private StatsFormatter() {
    }

    public static List<String> format(
            SkillRegistryView registry,
            Map<NamespacedId, ProgressionSnapshot> progression) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(progression, "progression");
        List<String> lines = new ArrayList<>();
        lines.add("[mcMMO] Stats");
        lines.add("Child skills are not counted toward Power Level.");

        int powerLevel = 0;
        for (SkillCategory category : List.of(
                SkillCategory.GATHERING,
                SkillCategory.COMBAT,
                SkillCategory.MOVEMENT,
                SkillCategory.UTILITY,
                SkillCategory.ADDON)) {
            List<ProgressionSnapshot> categoryProgress = registry.skills().stream()
                    .filter(skill -> !skill.childSkill())
                    .filter(skill -> skill.category() == category)
                    .map(skill -> progression.get(skill.id()))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ProgressionSnapshot::skillId))
                    .toList();
            if (categoryProgress.isEmpty()) {
                continue;
            }
            lines.add(categoryName(category) + ':');
            for (ProgressionSnapshot snapshot : categoryProgress) {
                lines.add("  " + SkillArgumentResolver.displayName(snapshot.skillId())
                        + " " + snapshot.level()
                        + " (" + snapshot.xp() + "/" + snapshot.xpToNextLevel() + " XP)");
                powerLevel = Math.addExact(powerLevel, snapshot.level());
            }
        }
        lines.add("POWER LEVEL: " + powerLevel);
        return List.copyOf(lines);
    }

    private static String categoryName(SkillCategory category) {
        return switch (category) {
            case GATHERING -> "Gathering Skills";
            case COMBAT -> "Combat Skills";
            case MOVEMENT -> "Movement Skills";
            case UTILITY -> "Misc Skills";
            case CHILD -> "Child Skills";
            case ADDON -> "Addon Skills";
        };
    }
}
