package io.github.njw3995.fabricmmo.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatsFormatterTest {
    @Test
    void excludesChildSkillsFromPowerLevel() {
        DefaultSkillRegistry registry = new DefaultSkillRegistry();
        CoreSkills.registerAll(registry);
        UUID playerId = UUID.randomUUID();
        Map<NamespacedId, ProgressionSnapshot> progression = Map.of(
                CoreSkills.MINING, new ProgressionSnapshot(playerId, CoreSkills.MINING, 12, 3, 1020),
                CoreSkills.SMELTING, new ProgressionSnapshot(playerId, CoreSkills.SMELTING, 20, 0, 1020));

        var lines = StatsFormatter.format(registry, progression);

        assertTrue(lines.stream().anyMatch(line -> line.contains("Mining 12")));
        assertEquals("POWER LEVEL: 12", lines.getLast());
    }
}
