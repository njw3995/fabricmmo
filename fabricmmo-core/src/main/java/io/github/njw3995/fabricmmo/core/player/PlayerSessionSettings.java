package io.github.njw3995.fabricmmo.core.player;

/** Session-only player toggles, matching mcMMO's online McMMOPlayer fields. */
public record PlayerSessionSettings(
        boolean abilityUse,
        boolean chatNotifications,
        boolean levelUpSounds,
        boolean debugMode) {
    public static final PlayerSessionSettings DEFAULTS =
            new PlayerSessionSettings(true, true, true, false);

    public PlayerSessionSettings withAbilityUse(boolean enabled) {
        return new PlayerSessionSettings(enabled, chatNotifications, levelUpSounds, debugMode);
    }

    public PlayerSessionSettings withChatNotifications(boolean enabled) {
        return new PlayerSessionSettings(abilityUse, enabled, levelUpSounds, debugMode);
    }

    public PlayerSessionSettings withLevelUpSounds(boolean enabled) {
        return new PlayerSessionSettings(abilityUse, chatNotifications, enabled, debugMode);
    }

    public PlayerSessionSettings withDebugMode(boolean enabled) {
        return new PlayerSessionSettings(abilityUse, chatNotifications, levelUpSounds, enabled);
    }
}
