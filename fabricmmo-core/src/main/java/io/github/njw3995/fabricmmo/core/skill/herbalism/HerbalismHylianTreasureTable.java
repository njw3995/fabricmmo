package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Strict loader for the Hylian_Luck section of upstream-compatible treasures.yml. */
public final class HerbalismHylianTreasureTable {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/HylianLuck");
    private static final Set<String> BUSHES = Set.of(
            "fern", "short_grass", "grass", "dead_bush",
            "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
            "acacia_sapling", "dark_oak_sapling", "mangrove_propagule", "cherry_sapling");
    private static final Set<String> FLOWERS = Set.of(
            "poppy", "dandelion", "blue_orchid", "allium", "azure_bluet",
            "orange_tulip", "pink_tulip", "red_tulip", "white_tulip");
    private final Map<NamespacedId, List<HerbalismHylianTreasure>> byBlock;

    private HerbalismHylianTreasureTable(
            Map<NamespacedId, List<HerbalismHylianTreasure>> byBlock) {
        LinkedHashMap<NamespacedId, List<HerbalismHylianTreasure>> copy = new LinkedHashMap<>();
        byBlock.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        this.byBlock = Map.copyOf(copy);
    }

    public static HerbalismHylianTreasureTable load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Missing FabricMMO configuration: " + file);
        }
        List<EntryBuilder> entries = parse(file);
        Map<NamespacedId, List<HerbalismHylianTreasure>> byBlock = new HashMap<>();
        int incompatible = 0;
        for (EntryBuilder builder : entries) {
            Identifier itemId = Identifier.of("minecraft", configName(builder.key));
            Item item = Registries.ITEM.getOrEmpty(itemId).orElse(null);
            if (item == null) {
                incompatible++;
                continue;
            }
            HerbalismHylianTreasure treasure = builder.build(
                    NamespacedId.parse(itemId.toString()));
            for (String source : builder.sources) {
                for (NamespacedId block : expandSource(source)) {
                    byBlock.computeIfAbsent(block, ignored -> new ArrayList<>()).add(treasure);
                }
            }
        }
        LOGGER.info("Loaded {} Hylian Luck treasures ({} incompatible with Minecraft 1.21.1)",
                entries.size() - incompatible, incompatible);
        return new HerbalismHylianTreasureTable(byBlock);
    }

    public List<HerbalismHylianTreasure> treasures(NamespacedId blockId) {
        return byBlock.getOrDefault(blockId, List.of());
    }

    private static Set<NamespacedId> expandSource(String source) {
        LinkedHashSet<NamespacedId> result = new LinkedHashSet<>();
        if (source.equals("Bushes")) {
            BUSHES.forEach(path -> result.add(new NamespacedId("minecraft", path)));
        } else if (source.equals("Flowers")) {
            FLOWERS.forEach(path -> result.add(new NamespacedId("minecraft", path)));
        } else if (source.equals("Pots")) {
            Registries.BLOCK.getIds().stream()
                    .filter(id -> id.getNamespace().equals("minecraft")
                            && id.getPath().startsWith("potted_"))
                    .forEach(id -> result.add(NamespacedId.parse(id.toString())));
        } else {
            result.add(new NamespacedId("minecraft", configName(source)));
        }
        return result;
    }

    private static List<EntryBuilder> parse(Path file) throws IOException {
        ArrayList<EntryBuilder> entries = new ArrayList<>();
        boolean section = false;
        EntryBuilder current = null;
        boolean levelRequirement = false;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String stripped = stripComment(line);
                if (stripped.isBlank()) {
                    continue;
                }
                int indent = leadingSpaces(stripped);
                String trimmed = stripped.trim();
                if (indent == 0) {
                    if (trimmed.equals("Hylian_Luck:")) {
                        section = true;
                        current = null;
                        continue;
                    }
                    if (section) {
                        break;
                    }
                    continue;
                }
                if (!section) {
                    continue;
                }
                if (indent == 4 && trimmed.endsWith(":")) {
                    current = new EntryBuilder(trimmed.substring(0, trimmed.length() - 1));
                    entries.add(current);
                    levelRequirement = false;
                    continue;
                }
                if (current == null) {
                    throw new IOException("Invalid Hylian Luck structure in " + file
                            + " at line " + lineNumber);
                }
                int separator = trimmed.indexOf(':');
                if (separator <= 0) {
                    throw new IOException("Invalid Hylian Luck value in " + file
                            + " at line " + lineNumber);
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (indent == 8 && key.equals("Level_Requirement")) {
                    levelRequirement = true;
                    continue;
                }
                if (indent == 8) {
                    levelRequirement = false;
                    switch (key) {
                        case "Amount" -> current.amount = parseInt(value, file, lineNumber);
                        case "XP" -> current.xp = parseInt(value, file, lineNumber);
                        case "Drop_Chance" -> current.chance = parseDouble(value, file, lineNumber);
                        case "Drops_From" -> current.sources.addAll(parseSources(value));
                        default -> throw new IOException("Unknown Hylian Luck key " + key
                                + " in " + file + " at line " + lineNumber);
                    }
                } else if (indent == 12 && levelRequirement) {
                    if (key.equals("Standard_Mode")) {
                        current.standardLevel = parseInt(value, file, lineNumber);
                    } else if (key.equals("Retro_Mode")) {
                        current.retroLevel = parseInt(value, file, lineNumber);
                    } else {
                        throw new IOException("Unknown Hylian Luck level key " + key
                                + " in " + file + " at line " + lineNumber);
                    }
                }
            }
        }
        for (EntryBuilder entry : entries) {
            entry.validate(file);
        }
        return List.copyOf(entries);
    }

    private static Set<String> parseSources(String value) throws IOException {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new IOException("Drops_From must be an inline list: " + value);
        }
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        String content = value.substring(1, value.length() - 1).trim();
        if (!content.isEmpty()) {
            for (String token : content.split(",")) {
                sources.add(token.trim());
            }
        }
        return sources;
    }

    private static String configName(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static int parseInt(String value, Path file, int line) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid integer in " + file + " at line " + line, exception);
        }
    }

    private static double parseDouble(String value, Path file, int line) throws IOException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid decimal in " + file + " at line " + line, exception);
        }
    }

    private static int leadingSpaces(String value) {
        int count = 0;
        while (count < value.length() && value.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String stripComment(String line) {
        int index = line.indexOf('#');
        return index < 0 ? line : line.substring(0, index);
    }

    private static final class EntryBuilder {
        private final String key;
        private int amount = -1;
        private int xp = -1;
        private double chance = -1.0D;
        private int standardLevel = -1;
        private int retroLevel = -1;
        private final Set<String> sources = new LinkedHashSet<>();

        private EntryBuilder(String key) {
            this.key = key;
        }

        private void validate(Path file) throws IOException {
            if (amount <= 0 || xp < 0 || chance < 0.0D || standardLevel < 0
                    || retroLevel < 0 || sources.isEmpty()) {
                throw new IOException("Incomplete Hylian Luck treasure " + key + " in " + file);
            }
        }

        private HerbalismHylianTreasure build(NamespacedId itemId) {
            return new HerbalismHylianTreasure(
                    key, itemId, amount, xp, chance, standardLevel, retroLevel);
        }
    }
}
