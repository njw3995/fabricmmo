package io.github.njw3995.fabricmmo.core.skill.alchemy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Strict specialized loader for the upstream potions.yml sequence/map structure. */
public final class AlchemyPotionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO");
    private final List<Set<Identifier>> ingredientsByTier;
    private final Map<String, AlchemyPotionDefinition> potions;

    private AlchemyPotionConfig(List<Set<Identifier>> ingredientsByTier,
                                Map<String, AlchemyPotionDefinition> potions) {
        this.ingredientsByTier = List.copyOf(ingredientsByTier);
        this.potions = Collections.unmodifiableMap(new LinkedHashMap<>(potions));
    }

    public static AlchemyPotionConfig load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("Missing FabricMMO potion configuration: " + file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<LinkedHashSet<Identifier>> rawTiers = new ArrayList<>();
        for (int i = 0; i < 8; i++) rawTiers.add(new LinkedHashSet<>());
        Map<String, MutablePotion> mutable = new LinkedHashMap<>();
        String section = "";
        int currentTier = -1;
        MutablePotion current = null;
        String subsection = "";
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = stripComment(lines.get(lineIndex));
            if (line.isBlank()) continue;
            if (line.indexOf('\t') >= 0) throw new IllegalArgumentException("Tabs are not supported in potions.yml line " + (lineIndex + 1));
            int indent = leadingSpaces(line);
            String trimmed = line.trim();
            if (indent == 0 && trimmed.endsWith(":")) {
                section = trimmed.substring(0, trimmed.length() - 1);
                currentTier = -1;
                current = null;
                subsection = "";
                continue;
            }
            if (section.equals("Concoctions")) {
                if (indent == 4 && trimmed.endsWith(":")) {
                    currentTier = tierIndex(trimmed.substring(0, trimmed.length() - 1));
                } else if (indent >= 8 && trimmed.startsWith("- ") && currentTier >= 0) {
                    rawTiers.get(currentTier).add(materialId(unquote(trimmed.substring(2).trim())));
                }
                continue;
            }
            if (!section.equals("Potions")) continue;
            if (indent == 4 && trimmed.endsWith(":")) {
                String id = trimmed.substring(0, trimmed.length() - 1);
                current = new MutablePotion(id);
                mutable.put(id, current);
                subsection = "";
                continue;
            }
            if (current == null) continue;
            if (indent == 8 && trimmed.endsWith(":")) {
                subsection = trimmed.substring(0, trimmed.length() - 1);
                continue;
            }
            if (indent == 8) {
                subsection = "";
                KeyValue kv = keyValue(trimmed, lineIndex);
                switch (kv.key()) {
                    case "Material" -> current.material = materialId(kv.value());
                    case "Name" -> current.name = unquote(kv.value());
                    case "Color" -> current.color = Integer.decode(kv.value());
                    case "Lore" -> current.lore.addAll(parseInlineList(kv.value()));
                    case "Effects" -> parseInlineList(kv.value()).stream()
                            .map(AlchemyEffectDefinition::parse).forEach(current.effects::add);
                    default -> { }
                }
                continue;
            }
            if (indent >= 12 && subsection.equals("PotionData")) {
                KeyValue kv = keyValue(trimmed, lineIndex);
                switch (kv.key()) {
                    case "PotionType" -> current.potionType = unquote(kv.value());
                    case "Extended" -> current.extended = Boolean.parseBoolean(kv.value());
                    case "Upgraded" -> current.upgraded = Boolean.parseBoolean(kv.value());
                    default -> { }
                }
            } else if (indent >= 12 && subsection.equals("Children")) {
                KeyValue kv = keyValue(trimmed, lineIndex);
                current.children.put(materialId(kv.key()), unquote(kv.value()));
            } else if (indent >= 12 && subsection.equals("Effects") && trimmed.startsWith("- ")) {
                current.effects.add(AlchemyEffectDefinition.parse(unquote(trimmed.substring(2).trim())));
            } else if (indent >= 12 && subsection.equals("Lore") && trimmed.startsWith("- ")) {
                current.lore.add(unquote(trimmed.substring(2).trim()));
            }
        }
        List<Set<Identifier>> cumulative = new ArrayList<>();
        LinkedHashSet<Identifier> running = new LinkedHashSet<>();
        for (Set<Identifier> tier : rawTiers) {
            running.addAll(tier);
            cumulative.add(Collections.unmodifiableSet(new LinkedHashSet<>(running)));
        }
        Map<String, AlchemyPotionDefinition> potions = new LinkedHashMap<>();
        for (MutablePotion potion : mutable.values()) {
            if (potion.potionType == null || potion.potionType.isBlank()) {
                throw new IllegalArgumentException("Missing PotionData.PotionType for " + potion.id);
            }
            AlchemyPotionDefinition definition = potion.freeze();
            potions.put(definition.id(), definition);
        }
        for (AlchemyPotionDefinition potion : potions.values()) {
            for (String child : potion.children().values()) {
                if (!potions.containsKey(child)) {
                    // Upstream loads unresolved child identifiers and simply returns no recipe
                    // when they are used. Report the corruption without discarding the rest of
                    // the graph; this also preserves the shipped 2.3.000 wind-charging typo.
                    LOGGER.warn("Alchemy potion {} references missing child {} in {}",
                            potion.id(), child, file);
                }
            }
        }
        return new AlchemyPotionConfig(cumulative, potions);
    }

    public Set<Identifier> ingredientsForTier(int tier) {
        return ingredientsByTier.get(Math.max(1, Math.min(8, tier)) - 1);
    }
    public Map<String, AlchemyPotionDefinition> potions() { return potions; }
    public AlchemyPotionDefinition potion(String id) { return potions.get(id); }
    public AlchemyPotionDefinition child(AlchemyPotionDefinition input, Identifier ingredient) {
        String child = input.children().get(ingredient);
        return child == null ? null : potions.get(child);
    }

    private static int tierIndex(String key) {
        return switch (key) {
            case "Tier_One_Ingredients" -> 0;
            case "Tier_Two_Ingredients" -> 1;
            case "Tier_Three_Ingredients" -> 2;
            case "Tier_Four_Ingredients" -> 3;
            case "Tier_Five_Ingredients" -> 4;
            case "Tier_Six_Ingredients" -> 5;
            case "Tier_Seven_Ingredients" -> 6;
            case "Tier_Eight_Ingredients" -> 7;
            default -> -1;
        };
    }
    private static Identifier materialId(String material) {
        String value = unquote(material).trim().toLowerCase(Locale.ROOT);
        Identifier parsed = Identifier.tryParse(value.contains(":") ? value : "minecraft:" + value);
        if (parsed == null) throw new IllegalArgumentException("Invalid item identifier: " + material);
        return parsed;
    }
    private static KeyValue keyValue(String value, int lineIndex) {
        int separator = value.indexOf(':');
        if (separator <= 0) throw new IllegalArgumentException("Invalid potions.yml mapping at line " + (lineIndex + 1));
        return new KeyValue(value.substring(0, separator).trim(), value.substring(separator + 1).trim());
    }
    private static List<String> parseInlineList(String raw) {
        String value = raw.trim();
        if (!value.startsWith("[") || !value.endsWith("]")) return List.of();
        value = value.substring(1, value.length() - 1).trim();
        if (value.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        for (String part : value.split(",")) result.add(unquote(part.trim()));
        return result;
    }
    private static String stripComment(String line) {
        boolean single = false, dbl = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == '#' && !single && !dbl) return line.substring(0, i);
        }
        return line;
    }
    private static String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) return value.substring(1, value.length() - 1);
        return value;
    }
    private static int leadingSpaces(String line) {
        int value = 0;
        while (value < line.length() && line.charAt(value) == ' ') value++;
        return value;
    }
    private record KeyValue(String key, String value) {}
    private static final class MutablePotion {
        final String id;
        Identifier material = Identifier.of("minecraft", "potion");
        String potionType;
        boolean extended;
        boolean upgraded;
        String name;
        Integer color;
        final List<String> lore = new ArrayList<>();
        final List<AlchemyEffectDefinition> effects = new ArrayList<>();
        final Map<Identifier, String> children = new LinkedHashMap<>();
        MutablePotion(String id) { this.id = id; }
        AlchemyPotionDefinition freeze() {
            if (extended && upgraded) upgraded = false;
            return new AlchemyPotionDefinition(id, material, potionType, extended, upgraded,
                    name, color, lore, effects, children);
        }
    }
}
