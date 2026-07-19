package io.github.njw3995.fabricmmo.core.skill.gathering;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolActivationBlockFilterTest {
    @Test
    void blocksInteractablesAndAxeTransformableWood() {
        assertFalse(ToolActivationBlockFilter.canActivateTools("oak_log"));
        assertFalse(ToolActivationBlockFilter.canActivateTools("stripped_crimson_hyphae"));
        assertFalse(ToolActivationBlockFilter.canActivateTools("crafting_table"));
        assertFalse(ToolActivationBlockFilter.canActivateTools("oak_trapdoor"));
        assertFalse(ToolActivationBlockFilter.canActivateTools("red_shulker_box"));
    }

    @Test
    void allowsOrdinaryNonInteractableBlocks() {
        assertTrue(ToolActivationBlockFilter.canActivateTools("stone"));
        assertTrue(ToolActivationBlockFilter.canActivateTools("dirt"));
        assertTrue(ToolActivationBlockFilter.canActivateTools("oak_planks"));
    }
}
