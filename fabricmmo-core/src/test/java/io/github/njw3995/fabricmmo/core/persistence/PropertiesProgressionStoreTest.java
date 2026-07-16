package io.github.njw3995.fabricmmo.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesProgressionStoreTest {
    @TempDir
    Path directory;

    @Test
    void roundTripsUnknownAddonSkills() throws Exception {
        UUID playerId = UUID.randomUUID();
        NamespacedId addonSkill = NamespacedId.parse("example:alchemy_plus");
        PropertiesProgressionStore store = new PropertiesProgressionStore(directory);
        store.save(new PlayerProgressionData(playerId, 4,
                Map.of(addonSkill, new StoredSkillProgress(12, 345))));
        PlayerProgressionData loaded = store.load(playerId);
        assertEquals(4, loaded.revision());
        assertEquals(new StoredSkillProgress(12, 345), loaded.skills().get(addonSkill));
    }
}
