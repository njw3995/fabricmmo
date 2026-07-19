package io.github.njw3995.fabricmmo.core.skill.excavation;

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

/** Strict loader for the Excavation section of upstream-compatible treasures.yml. */
public final class ExcavationTreasureTable {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/ExcavationTreasures");
    private final Map<NamespacedId, List<ExcavationTreasure>> byBlock;

    private ExcavationTreasureTable(Map<NamespacedId, List<ExcavationTreasure>> byBlock) {
        LinkedHashMap<NamespacedId, List<ExcavationTreasure>> copy = new LinkedHashMap<>();
        byBlock.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        this.byBlock = Map.copyOf(copy);
    }

    static ExcavationTreasureTable of(
            Map<NamespacedId, List<ExcavationTreasure>> byBlock) {
        return new ExcavationTreasureTable(byBlock);
    }

    public static ExcavationTreasureTable load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Missing FabricMMO configuration: " + file);
        }
        List<EntryBuilder> entries = parse(file);
        Map<NamespacedId, List<ExcavationTreasure>> byBlock = new HashMap<>();
        int incompatible = 0;
        for (EntryBuilder builder : entries) {
            Identifier itemId = Identifier.of("minecraft", configName(builder.key));
            Item item = Registries.ITEM.getOrEmpty(itemId).orElse(null);
            if (item == null) {
                incompatible++;
                continue;
            }
            ExcavationTreasure treasure = builder.build(NamespacedId.parse(itemId.toString()));
            for (NamespacedId block : treasure.dropsFrom()) {
                byBlock.computeIfAbsent(block, ignored -> new ArrayList<>()).add(treasure);
            }
        }
        LOGGER.info("Loaded {} Excavation treasures ({} incompatible with Minecraft 1.21.1)",
                entries.size() - incompatible, incompatible);
        return new ExcavationTreasureTable(byBlock);
    }

    public List<ExcavationTreasure> treasures(NamespacedId blockId) {
        return byBlock.getOrDefault(blockId, List.of());
    }

    public int configuredBlockCount() {
        return byBlock.size();
    }

    private static List<EntryBuilder> parse(Path file) throws IOException {
        ArrayList<EntryBuilder> entries = new ArrayList<>();
        boolean excavation = false;
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
                    if (trimmed.equals("Excavation:")) {
                        excavation = true;
                        current = null;
                        continue;
                    }
                    if (excavation) {
                        break;
                    }
                    continue;
                }
                if (!excavation) {
                    continue;
                }
                if (indent == 4 && trimmed.endsWith(":")) {
                    current = new EntryBuilder(trimmed.substring(0, trimmed.length() - 1));
                    entries.add(current);
                    levelRequirement = false;
                    continue;
                }
                if (current == null) {
                    throw new IOException("Invalid Excavation treasure structure in " + file
                            + " at line " + lineNumber);
                }
                int separator = trimmed.indexOf(':');
                if (separator <= 0) {
                    throw new IOException("Invalid Excavation treasure value in " + file
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
                        case "Drops_From" -> current.blocks.addAll(parseBlocks(value));
                        default -> throw new IOException("Unknown Excavation treasure key " + key
                                + " in " + file + " at line " + lineNumber);
                    }
                } else if (indent == 12 && levelRequirement) {
                    if (key.equals("Standard_Mode")) {
                        current.standardLevel = parseInt(value, file, lineNumber);
                    } else if (key.equals("Retro_Mode")) {
                        current.retroLevel = parseInt(value, file, lineNumber);
                    } else {
                        throw new IOException("Unknown level requirement key " + key + " in " + file
                                + " at line " + lineNumber);
                    }
                } else {
                    throw new IOException("Unsupported Excavation treasure indentation in " + file
                            + " at line " + lineNumber);
                }
            }
        }
        for (EntryBuilder entry : entries) {
            entry.validate(file);
        }
        return List.copyOf(entries);
    }

    private static Set<NamespacedId> parseBlocks(String value) throws IOException {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new IOException("Drops_From must be an inline list: " + value);
        }
        LinkedHashSet<NamespacedId> blocks = new LinkedHashSet<>();
        String content = value.substring(1, value.length() - 1).trim();
        if (content.isEmpty()) {
            return blocks;
        }
        for (String token : content.split(",")) {
            blocks.add(new NamespacedId("minecraft", configName(token.trim())));
        }
        return blocks;
    }

    private static String configName(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static int parseInt(String value, Path file, int line) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid integer in " + file + " at line " + line + ": " + value,
                    exception);
        }
    }

    private static double parseDouble(String value, Path file, int line) throws IOException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid decimal in " + file + " at line " + line + ": " + value,
                    exception);
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
        private final Set<NamespacedId> blocks = new LinkedHashSet<>();

        private EntryBuilder(String key) {
            this.key = key;
        }

        private void validate(Path file) throws IOException {
            if (amount <= 0 || xp < 0 || chance < 0.0D || standardLevel < 0
                    || retroLevel < 0 || blocks.isEmpty()) {
                throw new IOException("Incomplete Excavation treasure " + key + " in " + file);
            }
        }

        private ExcavationTreasure build(NamespacedId itemId) {
            return new ExcavationTreasure(
                    key, itemId, amount, xp, chance, standardLevel, retroLevel, blocks);
        }
    }
}
