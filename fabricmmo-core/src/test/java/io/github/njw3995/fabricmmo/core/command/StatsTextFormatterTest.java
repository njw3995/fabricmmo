package io.github.njw3995.fabricmmo.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.core.registry.DefaultSkillRegistry;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

class StatsTextFormatterTest {
    @Test
    void matchesUpstreamChatGroupingAndIncludesChildSkills() {
        DefaultSkillRegistry registry = new DefaultSkillRegistry();
        CoreSkills.registerAll(registry);
        UUID playerId = UUID.randomUUID();
        Map<NamespacedId, ProgressionSnapshot> progression = Map.of(
                CoreSkills.MINING,
                new ProgressionSnapshot(playerId, CoreSkills.MINING, 12, 3, 1020),
                CoreSkills.SMELTING,
                new ProgressionSnapshot(playerId, CoreSkills.SMELTING, 6, 0, 1020));

        var lines = StatsTextFormatter.format(registry, progression);

        assertEquals("[mcMMO] Stats", lines.getFirst().getString());
        assertEquals(Formatting.GREEN.getColorValue(),
                lines.getFirst().getStyle().getColor().getRgb());
        assertTrue(lines.stream().anyMatch(line -> line.getString().equals("-=GATHERING SKILLS=-")));
        assertTrue(lines.stream().anyMatch(line -> line.getString().equals("Mining: 12 XP(3/1020)")));
        assertTrue(lines.stream().anyMatch(line -> line.getString().equals("Smelting: 6")));
        assertEquals("POWER LEVEL: 12", lines.getLast().getString());
    }
}
