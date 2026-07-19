package io.github.njw3995.fabricmmo.core.skill.gathering;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Strict reusable loader for {@code Experience_Values.<Skill>} block XP mappings. */
public final class ConfiguredBlockXpTable {
    private final String skillName;
    private final Map<NamespacedId, Integer> xpByBlock;

    private ConfiguredBlockXpTable(String skillName, Map<NamespacedId, Integer> xpByBlock) {
        this.skillName = skillName;
        this.xpByBlock = Collections.unmodifiableMap(new TreeMap<>(xpByBlock));
    }

    public static ConfiguredBlockXpTable load(Path experienceFile, String skillName)
            throws IOException {
        return load(experienceFile, skillName, Map.of());
    }

    public static ConfiguredBlockXpTable load(
            Path experienceFile,
            String skillName,
            Map<NamespacedId, Integer> fallbackValues) throws IOException {
        Objects.requireNonNull(experienceFile, "experienceFile");
        String normalizedSkill = requireName(skillName);
        FlatYamlConfig yaml = FlatYamlConfig.load(experienceFile);
        String prefix = "Experience_Values." + normalizedSkill + '.';
        Map<NamespacedId, Integer> values = new TreeMap<>();
        Objects.requireNonNull(fallbackValues, "fallbackValues").forEach((id, xp) -> {
            Objects.requireNonNull(id, "fallback block id");
            Objects.requireNonNull(xp, "fallback XP");
            if (xp < 0) {
                throw new IllegalArgumentException(
                        normalizedSkill + " fallback XP must not be negative for " + id);
            }
            values.put(id, xp);
        });
        yaml.valuesWithPrefix(prefix).forEach((path, rawValue) -> {
            NamespacedId id = materialId(path.substring(prefix.length()));
            int xp;
            try {
                xp = Integer.parseInt(rawValue);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid " + normalizedSkill + " XP value for " + id + ": " + rawValue,
                        exception);
            }
            if (xp < 0) {
                throw new IllegalArgumentException(
                        normalizedSkill + " XP must not be negative for " + id);
            }
            values.put(id, xp);
        });
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    "Experience_Values." + normalizedSkill + " must contain at least one entry");
        }
        return new ConfiguredBlockXpTable(normalizedSkill, values);
    }

    public int xpFor(NamespacedId blockId) {
        return xpByBlock.getOrDefault(Objects.requireNonNull(blockId, "blockId"), 0);
    }

    public boolean contains(NamespacedId blockId) {
        return xpFor(blockId) > 0;
    }

    public String skillName() {
        return skillName;
    }

    public Map<NamespacedId, Integer> entries() {
        return xpByBlock;
    }

    public static NamespacedId materialId(String key) {
        String normalized = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Material key must not be empty");
        }
        return normalized.indexOf(':') >= 0
                ? NamespacedId.parse(normalized)
                : new NamespacedId("minecraft", normalized);
    }

    private static String requireName(String value) {
        String trimmed = Objects.requireNonNull(value, "skillName").trim();
        if (trimmed.isEmpty() || trimmed.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Invalid skill configuration name: " + value);
        }
        return trimmed;
    }
}
