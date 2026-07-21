package io.github.njw3995.fabricmmo.core.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.BrewingContentDefinition;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentDefinition;
import io.github.njw3995.fabricmmo.api.content.GatheringContentDefinition;
import io.github.njw3995.fabricmmo.api.content.MaturityRequirement;
import io.github.njw3995.fabricmmo.api.content.ReplantDefinition;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.util.Identifier;

/** Strict parser for reloadable FabricMMO content declarations. */
public final class DatapackContentParser {
    private DatapackContentParser() {}

    public static Parsed<GatheringContentDefinition> gathering(Identifier resourceId, Reader reader) {
        JsonObject root = object(JsonParser.parseReader(reader), "root");
        validateFormat(root);
        rejectUnknownKeys(root, "root", Set.of(
                "format", "id", "enabled", "skill", "block", "xp", "valid_tools",
                "natural_blocks_only", "maturity", "bonus_drops", "active_ability",
                "replant", "metadata"));
        NamespacedId id = definitionId(resourceId, "fabricmmo/gathering/", root);
        if (!booleanValue(root, "enabled", true)) return Parsed.disabled(id);
        NamespacedId skill = NamespacedId.parse(requiredString(root, "skill"));
        ContentSelector block = selector(requiredString(root, "block"));
        int xp = nonNegativeInt(root, "xp");
        Set<ContentSelector> tools = selectors(root, "valid_tools");
        boolean naturalOnly = booleanValue(root, "natural_blocks_only", true);
        MaturityRequirement maturity = maturity(root.getAsJsonObject("maturity"));
        boolean bonusDrops = booleanValue(root, "bonus_drops", false);
        boolean activeAbility = booleanValue(root, "active_ability", false);
        Optional<ReplantDefinition> replant = root.has("replant")
                ? Optional.of(replant(object(root.get("replant"), "replant")))
                : Optional.empty();
        Map<String, String> metadata = metadata(root.getAsJsonObject("metadata"));
        return Parsed.enabled(new GatheringContentDefinition(
                id, skill, block, xp, tools, naturalOnly, maturity,
                bonusDrops, activeAbility, replant, metadata));
    }

    public static Parsed<EntityXpContentDefinition> entityXp(
            Identifier resourceId,
            Reader reader) {
        JsonObject root = object(JsonParser.parseReader(reader), "root");
        validateFormat(root);
        rejectUnknownKeys(root, "root", Set.of(
                "format", "id", "enabled", "scope", "entity", "xp", "metadata"));
        NamespacedId id = definitionId(resourceId, "fabricmmo/entity_xp/", root);
        if (!booleanValue(root, "enabled", true)) return Parsed.disabled(id);
        EntityXpContentDefinition.Scope scope;
        try {
            scope = EntityXpContentDefinition.Scope.valueOf(
                    requiredString(root, "scope").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown entity XP scope: " + root.get("scope"), exception);
        }
        return Parsed.enabled(new EntityXpContentDefinition(
                id,
                scope,
                selector(requiredString(root, "entity")),
                nonNegativeDouble(root, "xp"),
                metadata(root.getAsJsonObject("metadata"))));
    }

    public static Parsed<BrewingContentDefinition> brewing(Identifier resourceId, Reader reader) {
        JsonObject root = object(JsonParser.parseReader(reader), "root");
        validateFormat(root);
        rejectUnknownKeys(root, "root", Set.of(
                "format", "id", "enabled", "ingredient", "input", "output", "stage", "metadata"));
        NamespacedId id = definitionId(resourceId, "fabricmmo/brewing/", root);
        if (!booleanValue(root, "enabled", true)) return Parsed.disabled(id);
        return Parsed.enabled(new BrewingContentDefinition(
                id,
                selector(requiredString(root, "ingredient")),
                selector(requiredString(root, "input")),
                selector(requiredString(root, "output")),
                positiveInt(root, "stage"),
                metadata(root.getAsJsonObject("metadata"))));
    }


    private static void validateFormat(JsonObject root) {
        if (!root.has("format")) return;
        int format = positiveInt(root, "format");
        if (format != 1) {
            throw new IllegalArgumentException("Unsupported FabricMMO datapack format " + format);
        }
    }

    private static void rejectUnknownKeys(JsonObject object, String name, Set<String> allowed) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unknown key " + name + "." + key);
            }
        }
    }
    private static NamespacedId definitionId(
            Identifier resourceId, String prefix, JsonObject root) {
        if (root.has("id")) return NamespacedId.parse(requiredString(root, "id"));
        String path = resourceId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("Unexpected FabricMMO content path: " + resourceId);
        }
        String definitionPath = path.substring(prefix.length(), path.length() - ".json".length());
        return new NamespacedId(resourceId.getNamespace(), definitionPath);
    }

    private static ContentSelector selector(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("#")
                ? ContentSelector.tag(NamespacedId.parse(trimmed.substring(1)))
                : ContentSelector.id(NamespacedId.parse(trimmed));
    }

    private static Set<ContentSelector> selectors(JsonObject root, String key) {
        if (!root.has(key)) return Set.of();
        JsonElement element = root.get(key);
        if (!element.isJsonArray()) throw new IllegalArgumentException(key + " must be an array");
        TreeSet<ContentSelector> result = new TreeSet<>();
        element.getAsJsonArray().forEach(value -> result.add(selector(string(value, key))));
        return Set.copyOf(result);
    }

    private static MaturityRequirement maturity(JsonObject object) {
        if (object == null) return MaturityRequirement.any();
        rejectUnknownKeys(object, "maturity", Set.of("mode", "property", "value"));
        String mode = requiredString(object, "mode").toLowerCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "any" -> MaturityRequirement.any();
            case "maximum", "integer_property_maximum" ->
                    MaturityRequirement.maximum(requiredString(object, "property"));
            case "at_least", "integer_property_at_least" ->
                    MaturityRequirement.atLeast(
                            requiredString(object, "property"), nonNegativeInt(object, "value"));
            default -> throw new IllegalArgumentException("Unknown maturity mode: " + mode);
        };
    }

    private static ReplantDefinition replant(JsonObject object) {
        rejectUnknownKeys(object, "replant", Set.of(
                "planting_item", "age_property", "rank_ages",
                "active_ability_rank_bonus", "delay_ticks"));
        List<Integer> rankAges = new ArrayList<>();
        JsonElement ages = object.get("rank_ages");
        if (ages == null || !ages.isJsonArray()) {
            throw new IllegalArgumentException("replant.rank_ages must be an array");
        }
        ages.getAsJsonArray().forEach(value -> {
            int age = integer(value, "replant.rank_ages");
            if (age < 0) throw new IllegalArgumentException("replant.rank_ages must be non-negative");
            rankAges.add(age);
        });
        return new ReplantDefinition(
                selector(requiredString(object, "planting_item")),
                requiredString(object, "age_property"),
                rankAges,
                optionalNonNegativeInt(object, "active_ability_rank_bonus", 0),
                optionalNonNegativeInt(object, "delay_ticks", 1));
    }

    private static Map<String, String> metadata(JsonObject object) {
        if (object == null) return Map.of();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (var entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                throw new IllegalArgumentException("metadata." + entry.getKey() + " must be primitive");
            }
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(result);
    }

    private static JsonObject object(JsonElement element, String name) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(name + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private static String requiredString(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null) throw new IllegalArgumentException("Missing required key " + key);
        return string(value, key);
    }

    private static String string(JsonElement value, String key) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        String result = value.getAsString().trim();
        if (result.isEmpty()) throw new IllegalArgumentException(key + " must not be blank");
        return result;
    }

    private static int nonNegativeInt(JsonObject root, String key) {
        int value = integer(root, key);
        if (value < 0) throw new IllegalArgumentException(key + " must be non-negative");
        return value;
    }

    private static int positiveInt(JsonObject root, String key) {
        int value = integer(root, key);
        if (value <= 0) throw new IllegalArgumentException(key + " must be positive");
        return value;
    }

    private static int optionalNonNegativeInt(JsonObject root, String key, int fallback) {
        if (!root.has(key)) return fallback;
        return nonNegativeInt(root, key);
    }

    private static int integer(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null) throw new IllegalArgumentException(key + " must be an integer");
        return integer(value, key);
    }

    private static int integer(JsonElement value, String key) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        double raw = value.getAsDouble();
        int integer = value.getAsInt();
        if (!Double.isFinite(raw) || raw != integer) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        return integer;
    }

    private static double nonNegativeDouble(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        double result = value.getAsDouble();
        if (!Double.isFinite(result) || result < 0.0D) {
            throw new IllegalArgumentException(key + " must be finite and non-negative");
        }
        return result;
    }

    private static boolean booleanValue(JsonObject root, String key, boolean fallback) {
        if (!root.has(key)) return fallback;
        JsonElement value = root.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    public record Parsed<T>(NamespacedId id, Optional<T> definition) {
        public static <T> Parsed<T> enabled(T definition) {
            NamespacedId id;
            if (definition instanceof GatheringContentDefinition gathering) {
                id = gathering.id();
            } else if (definition instanceof EntityXpContentDefinition entityXp) {
                id = entityXp.id();
            } else if (definition instanceof BrewingContentDefinition brewing) {
                id = brewing.id();
            } else {
                throw new IllegalArgumentException(
                        "Unsupported FabricMMO datapack definition type: " + definition.getClass());
            }
            return new Parsed<>(id, Optional.of(definition));
        }

        public static <T> Parsed<T> disabled(NamespacedId id) {
            return new Parsed<>(id, Optional.empty());
        }
    }
}
