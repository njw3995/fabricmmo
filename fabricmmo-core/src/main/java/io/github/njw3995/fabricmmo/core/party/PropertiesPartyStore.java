package io.github.njw3995.fabricmmo.core.party;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** Atomic versioned party persistence, including migration from batch-2 format version 1. */
public final class PropertiesPartyStore implements PartyStore {
    private static final String FORMAT = "2";
    private final Path path;

    public PropertiesPartyStore(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    @Override
    public synchronized Map<String, PartyState> load() throws IOException {
        if (!Files.exists(path)) {
            return Map.of();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        String format = properties.getProperty("format.version");
        if (!"1".equals(format) && !FORMAT.equals(format)) {
            throw new IOException("Unsupported party format " + format + " in " + path);
        }
        TreeMap<String, PartyState> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String names = properties.getProperty("party.names", "");
        for (String encodedName : names.split(",")) {
            if (encodedName.isBlank()) {
                continue;
            }
            String key = encodedName.trim();
            String prefix = "party." + key + ".";
            String name = required(properties, prefix + "name");
            UUID owner = UUID.fromString(required(properties, prefix + "owner"));
            Set<UUID> members = Arrays.stream(required(properties, prefix + "members").split(","))
                    .filter(value -> !value.isBlank())
                    .map(UUID::fromString)
                    .collect(Collectors.toCollection(TreeSet::new));
            Optional<String> password = Optional.ofNullable(properties.getProperty(prefix + "password"));
            boolean locked = Boolean.parseBoolean(properties.getProperty(prefix + "locked", "true"));
            int level = Integer.parseInt(properties.getProperty(prefix + "level", "0"));
            double xp = Double.parseDouble(properties.getProperty(prefix + "xp", "0.0"));
            ShareMode xpShare = migratedShareMode(properties.getProperty(prefix + "xpShare", "NONE"));
            ShareMode itemShare = "1".equals(format)
                    ? (Boolean.parseBoolean(properties.getProperty(prefix + "itemShare", "false"))
                            ? ShareMode.EQUAL : ShareMode.NONE)
                    : migratedShareMode(properties.getProperty(prefix + "itemShareMode", "NONE"));
            EnumSet<ItemShareCategory> categories = EnumSet.allOf(ItemShareCategory.class);
            if (FORMAT.equals(format)) {
                categories.clear();
                String configured = properties.getProperty(prefix + "itemShareCategories", "");
                for (String value : configured.split(",")) {
                    if (!value.isBlank()) {
                        categories.add(ItemShareCategory.parse(value));
                    }
                }
            }
            Optional<String> alliance = Optional.ofNullable(properties.getProperty(prefix + "alliance"));
            result.put(name, new PartyState(
                    name, owner, members, password, locked, level, xp, xpShare, itemShare,
                    categories, alliance));
        }
        return Map.copyOf(result);
    }

    @Override
    public synchronized void save(Map<String, PartyState> parties) throws IOException {
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.setProperty("format.version", FORMAT);
        TreeMap<String, PartyState> ordered = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ordered.putAll(parties);
        properties.setProperty("party.names", ordered.values().stream()
                .map(PartyState::name)
                .map(PropertiesPartyStore::encode)
                .collect(Collectors.joining(",")));
        for (PartyState party : ordered.values()) {
            String prefix = "party." + encode(party.name()) + ".";
            properties.setProperty(prefix + "name", party.name());
            properties.setProperty(prefix + "owner", party.owner().toString());
            properties.setProperty(prefix + "members", party.members().stream()
                    .sorted()
                    .map(UUID::toString)
                    .collect(Collectors.joining(",")));
            party.password().ifPresent(value -> properties.setProperty(prefix + "password", value));
            properties.setProperty(prefix + "locked", Boolean.toString(party.locked()));
            properties.setProperty(prefix + "level", Integer.toString(party.level()));
            properties.setProperty(prefix + "xp", Double.toString(party.xp()));
            properties.setProperty(prefix + "xpShare", party.xpShare().name());
            properties.setProperty(prefix + "itemShareMode", party.itemShare().name());
            properties.setProperty(prefix + "itemShareCategories", party.itemShareCategories().stream()
                    .sorted()
                    .map(Enum::name)
                    .collect(Collectors.joining(",")));
            party.alliance().ifPresent(value -> properties.setProperty(prefix + "alliance", value));
        }
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) {
            properties.store(output, "FabricMMO parties; format version 2");
        }
        try {
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static ShareMode migratedShareMode(String value) {
        if ("LEVEL".equalsIgnoreCase(value)) {
            return ShareMode.EQUAL;
        }
        return ShareMode.valueOf(value.toUpperCase(java.util.Locale.ROOT));
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IOException("Missing " + key + " in " + properties);
        }
        return value;
    }
}
