package io.github.njw3995.fabricmmo.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultProgressionServiceTest {
    @Test
    void awardsValidatedXpAndEmitsLevelEvent() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        UUID player = UUID.randomUUID();
        AtomicInteger levelEvents = new AtomicInteger();
        api.events().subscribe(LevelChangedEvent.class, ignored -> levelEvents.incrementAndGet());
        XpAwardResult result = api.progression().award(new XpAwardRequest(
                player,
                CoreSkills.MINING,
                CoreXpSources.MINING_BLOCK_BREAK,
                1020,
                Map.of()));
        assertEquals(XpAwardResult.Status.APPLIED, result.status());
        assertEquals(1, result.newLevel());
        assertEquals(1, levelEvents.get());
    }

    @Test
    void rejectsUnregisteredXpSources() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        XpAwardResult result = api.progression().award(new XpAwardRequest(
                UUID.randomUUID(),
                CoreSkills.MINING,
                NamespacedId.parse("unknown:source"),
                100,
                Map.of()));
        assertEquals(XpAwardResult.Status.REJECTED, result.status());
    }
}
