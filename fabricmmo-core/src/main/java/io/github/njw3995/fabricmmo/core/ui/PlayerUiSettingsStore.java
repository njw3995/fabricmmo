package io.github.njw3995.fabricmmo.core.ui;
import java.util.UUID;
public interface PlayerUiSettingsStore { PlayerUiSettings load(UUID id); void save(UUID id,PlayerUiSettings settings); void remove(UUID id); void clear(); }
