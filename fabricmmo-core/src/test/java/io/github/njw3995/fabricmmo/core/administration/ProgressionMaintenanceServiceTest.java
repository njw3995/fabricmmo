package io.github.njw3995.fabricmmo.core.administration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.persistence.MySqlSettings;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.PropertiesProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.StoredSkillProgress;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgressionMaintenanceServiceTest {
    @TempDir
    Path directory;

    @Test
    void purgesPowerlessAndInactiveRecords() throws Exception {
        Path players = directory.resolve("players");
        PropertiesProgressionStore store = new PropertiesProgressionStore(players);
        UUID powerless = UUID.randomUUID();
        UUID active = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        NamespacedId mining = NamespacedId.parse("fabricmmo:mining");
        store.save(PlayerProgressionData.empty(powerless));
        store.save(new PlayerProgressionData(active, 1,
                Map.of(mining, new StoredSkillProgress(5, 25.0D))));
        store.save(new PlayerProgressionData(inactive, 1,
                Map.of(mining, new StoredSkillProgress(7, 10.0D))));
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        store.touch(active, now.minusSeconds(60));
        store.touch(inactive, now.minusSeconds(400L * 24L * 60L * 60L));
        ProgressionMaintenanceService maintenance = new ProgressionMaintenanceService(
                store, players, disabledMysql(), Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(1, maintenance.purgePowerless().removedPlayers());
        assertFalse(store.playerIds().contains(powerless));
        assertEquals(1, maintenance.purgeOldUsers(6).removedPlayers());
        assertTrue(store.playerIds().contains(active));
        assertFalse(store.playerIds().contains(inactive));
    }

    private static MySqlSettings disabledMysql() {
        return new MySqlSettings(
                false, false, "localhost", 3306, "fabricmmo", "user", "password",
                "mcmmo_", false, false, 10);
    }
}
