package io.github.njw3995.fabricmmo.core.command;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class SkillArgumentResolver {
    private SkillArgumentResolver() {
    }

    public static Optional<SkillDefinition> resolve(
            SkillRegistryView registry,
            String argument,
            boolean allowChildSkills) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(argument, "argument");
        String normalized = argument.toLowerCase(Locale.ROOT);

        Optional<SkillDefinition> exact = resolveExact(registry, normalized);
        if (exact.isPresent()) {
            return exact.filter(skill -> allowChildSkills || !skill.childSkill());
        }

        if (!normalized.contains(":")) {
            Optional<SkillDefinition> coreShortName = registry.find(
                    new NamespacedId("fabricmmo", normalized));
            if (coreShortName.isPresent()) {
                return coreShortName.filter(skill -> allowChildSkills || !skill.childSkill());
            }
        }

        List<SkillDefinition> pathMatches = registry.skills().stream()
                .filter(skill -> skill.id().path().equals(normalized))
                .filter(skill -> allowChildSkills || !skill.childSkill())
                .toList();
        return pathMatches.size() == 1 ? Optional.of(pathMatches.getFirst()) : Optional.empty();
    }

    public static List<String> suggestions(SkillRegistryView registry, boolean includeAll) {
        Objects.requireNonNull(registry, "registry");
        var suggestions = new java.util.ArrayList<String>();
        if (includeAll) {
            suggestions.add("all");
        }
        registry.skills().stream()
                .filter(skill -> !skill.childSkill())
                .map(skill -> skill.id().namespace().equals("fabricmmo")
                        ? skill.id().path()
                        : skill.id().toString())
                .sorted()
                .forEach(suggestions::add);
        return List.copyOf(suggestions);
    }

    public static String displayName(NamespacedId id) {
        String[] words = id.path().replace('-', '_').split("_");
        StringBuilder display = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!display.isEmpty()) {
                display.append(' ');
            }
            display.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return display.toString();
    }

    private static Optional<SkillDefinition> resolveExact(SkillRegistryView registry, String argument) {
        if (!argument.contains(":")) {
            return Optional.empty();
        }
        try {
            return registry.find(NamespacedId.parse(argument));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
