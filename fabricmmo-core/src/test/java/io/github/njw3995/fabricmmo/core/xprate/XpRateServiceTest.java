package io.github.njw3995.fabricmmo.core.xprate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class XpRateServiceTest {
    private static final NamespacedId MINING = NamespacedId.parse("fabricmmo:mining");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-18T18:00:00Z"), ZoneOffset.UTC);

    @Test
    void quietSkillRateDoesNotStartEventMode() {
        XpRateService service = service();

        service.setSkill(MINING, 2.0D, false);

        assertFalse(service.snapshot().globalEvent());
    }

    @Test
    void eventSkillRateStartsEventMode() {
        XpRateService service = service();

        service.setSkill(MINING, 2.0D, true);

        assertTrue(service.snapshot().globalEvent());
    }

    private static XpRateService service() {
        return new XpRateService(1.0D, Map.of(MINING, 1.0D), CLOCK);
    }
}
