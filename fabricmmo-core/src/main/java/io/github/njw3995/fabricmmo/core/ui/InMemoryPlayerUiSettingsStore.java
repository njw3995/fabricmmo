package io.github.njw3995.fabricmmo.core.ui;
import java.util.HashMap;import java.util.Map;import java.util.UUID;
public final class InMemoryPlayerUiSettingsStore implements PlayerUiSettingsStore { private final Map<UUID,PlayerUiSettings> map=new HashMap<>(); public synchronized PlayerUiSettings load(UUID id){return map.getOrDefault(id,PlayerUiSettings.DEFAULTS);} public synchronized void save(UUID id,PlayerUiSettings s){map.put(id,s);} public synchronized void remove(UUID id){map.remove(id);} public synchronized void clear(){map.clear();} }
