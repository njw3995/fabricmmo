package io.github.njw3995.fabricmmo.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class YamlDefaultsMergerTest {
    @Test
    void insertsMissingNestedDefaultsWithoutChangingAdminValuesOrCustomKeys() throws Exception {
        List<String> existing = List.of(
                "Experience_Values:",
                "  Mining:",
                "    Coal_Ore: 9999",
                "    Custom_Admin_Block: 42",
                "Other: keep");
        List<String> defaults = List.of(
                "Experience_Values:",
                "  Mining:",
                "    Coal_Ore: 400",
                "    Diamond_Ore: 2400",
                "  Excavation:",
                "    Sand: 40",
                "    Dirt: 40",
                "Other: default");

        YamlDefaultsMerger.MergeResult result = YamlDefaultsMerger.merge(existing, defaults);
        String merged = String.join("\n", result.lines());

        assertTrue(result.changed());
        assertTrue(merged.contains("    Coal_Ore: 9999"));
        assertTrue(merged.contains("    Custom_Admin_Block: 42"));
        assertTrue(merged.contains("    Diamond_Ore: 2400"));
        assertTrue(merged.contains("  Excavation:"));
        assertTrue(merged.contains("    Sand: 40"));
        assertTrue(merged.contains("Other: keep"));
        assertFalse(merged.contains("Coal_Ore: 400"));
    }

    @Test
    void secondMergeIsIdempotent() throws Exception {
        List<String> existing = List.of("Root:", "  Existing: true");
        List<String> defaults = List.of("Root:", "  Existing: false", "  Added: 7");

        YamlDefaultsMerger.MergeResult first = YamlDefaultsMerger.merge(existing, defaults);
        YamlDefaultsMerger.MergeResult second = YamlDefaultsMerger.merge(first.lines(), defaults);

        assertTrue(first.changed());
        assertFalse(second.changed());
        assertEquals(first.lines(), second.lines());
    }
}
