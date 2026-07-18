package io.github.njw3995.fabricmmo.core.player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/** Small UUID/name index used by offline administration and flat-file leaderboards. */
public final class PlayerIdentityStore {
    private final Path path;
    private final Map<UUID, String> namesById = new HashMap<>();
    private final Map<String, UUID> idsByName = new HashMap<>();
    public PlayerIdentityStore(Path path) throws IOException { this.path = path.toAbsolutePath().normalize(); load(); }
    public synchronized void remember(UUID id, String name) {
        Objects.requireNonNull(id, "id"); String normalized = Objects.requireNonNull(name, "name").trim();
        if (normalized.isEmpty()) return;
        String old = namesById.put(id, normalized); if (old != null) idsByName.remove(old.toLowerCase(Locale.ROOT));
        idsByName.put(normalized.toLowerCase(Locale.ROOT), id); saveUnchecked();
    }
    public synchronized Optional<UUID> findUuid(String name) { return Optional.ofNullable(idsByName.get(name.toLowerCase(Locale.ROOT))); }
    public synchronized Optional<String> findName(UUID id) { return Optional.ofNullable(namesById.get(id)); }
    public synchronized Map<UUID,String> identities() { return Map.copyOf(namesById); }
    private void load() throws IOException {
        if (!Files.exists(path)) return; Properties p=new Properties(); try(InputStream in=Files.newInputStream(path)){p.load(in);}
        for(String key:p.stringPropertyNames()) if(key.startsWith("player.")){UUID id=UUID.fromString(key.substring(7)); String name=p.getProperty(key); namesById.put(id,name); idsByName.put(name.toLowerCase(Locale.ROOT),id);}
    }
    private void saveUnchecked(){try{save();}catch(IOException e){throw new java.io.UncheckedIOException(e);}}
    private void save() throws IOException { Files.createDirectories(path.getParent()); Properties p=new Properties(); namesById.forEach((id,name)->p.setProperty("player."+id,name)); try(OutputStream out=Files.newOutputStream(path)){p.store(out,"FabricMMO player identity index");} }
}
