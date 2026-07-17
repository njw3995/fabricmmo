package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningCommandFormatterTest {
    @Test
    void includesAllMiningAbilityFamilies() {
        MiningCommandSnapshot snapshot = new MiningCommandSnapshot(
                1000, 100, 200, 100.0D, 50.0D, 22, 0, true, 10,
                8, 8, 70.0D, 3, 4.0D, 100.0D, 0);
        var lines = MiningCommandFormatter.format(snapshot);

        assertTrue(lines.stream().anyMatch(line -> line.contains("Double Drops")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Mother Lode")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Super Breaker")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Blast Mining")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Demolitions")));
    }
    @Test
    void hidesLockedOrUnpermittedAbilitySections() {
        MiningCommandSnapshot snapshot = new MiningCommandSnapshot(
                0, 0, 100, 0.0D, 0.0D, 2, 0, false, 0,
                0, 8, 0.0D, 0, 0.0D, 0.0D, 0,
                false, false, false, false, false, false);
        var lines = MiningCommandFormatter.format(snapshot);

        assertTrue(lines.stream().anyMatch(line -> line.contains("Mining Level")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("Double Drops")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("Super Breaker")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("Blast Mining")));
    }

}
