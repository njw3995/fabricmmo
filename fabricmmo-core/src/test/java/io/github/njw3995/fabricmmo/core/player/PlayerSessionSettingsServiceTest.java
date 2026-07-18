package io.github.njw3995.fabricmmo.core.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerSessionSettingsServiceTest {
    @Test
    void defaultsMatchUpstreamOnlinePlayerState() {
        PlayerSessionSettingsService service = new PlayerSessionSettingsService();
        PlayerSessionSettings settings = service.get(UUID.randomUUID());

        assertTrue(settings.abilityUse());
        assertTrue(settings.chatNotifications());
        assertTrue(settings.levelUpSounds());
        assertFalse(settings.debugMode());
    }

    @Test
    void settingsResetWhenSessionIsRemoved() {
        PlayerSessionSettingsService service = new PlayerSessionSettingsService();
        UUID playerId = UUID.randomUUID();

        assertFalse(service.toggleAbilityUse(playerId).abilityUse());
        assertFalse(service.toggleChatNotifications(playerId).chatNotifications());
        assertFalse(service.toggleLevelUpSounds(playerId).levelUpSounds());
        service.remove(playerId);

        assertTrue(service.get(playerId).abilityUse());
        assertTrue(service.get(playerId).chatNotifications());
        assertTrue(service.get(playerId).levelUpSounds());
    }
}
