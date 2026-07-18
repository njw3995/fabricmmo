package io.github.njw3995.fabricmmo.core.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Owns mcMMO-compatible player toggles for the lifetime of an online session. */
public final class PlayerSessionSettingsService {
    private final Map<UUID, PlayerSessionSettings> settings = new HashMap<>();

    public synchronized PlayerSessionSettings get(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return settings.getOrDefault(playerId, PlayerSessionSettings.DEFAULTS);
    }

    public synchronized PlayerSessionSettings toggleAbilityUse(UUID playerId) {
        return update(playerId, current -> current.withAbilityUse(!current.abilityUse()));
    }

    public synchronized PlayerSessionSettings toggleChatNotifications(UUID playerId) {
        return update(playerId,
                current -> current.withChatNotifications(!current.chatNotifications()));
    }

    public synchronized PlayerSessionSettings toggleLevelUpSounds(UUID playerId) {
        return update(playerId, current -> current.withLevelUpSounds(!current.levelUpSounds()));
    }

    public synchronized PlayerSessionSettings toggleDebugMode(UUID playerId) {
        return update(playerId, current -> current.withDebugMode(!current.debugMode()));
    }

    public synchronized void remove(UUID playerId) {
        settings.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public synchronized void clear() {
        settings.clear();
    }

    private PlayerSessionSettings update(
            UUID playerId,
            UnaryOperator<PlayerSessionSettings> update) {
        Objects.requireNonNull(playerId, "playerId");
        PlayerSessionSettings changed = update.apply(get(playerId));
        settings.put(playerId, changed);
        return changed;
    }
}
