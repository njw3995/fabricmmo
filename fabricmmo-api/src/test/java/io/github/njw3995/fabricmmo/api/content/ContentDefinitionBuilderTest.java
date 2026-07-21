package io.github.njw3995.fabricmmo.api.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContentDefinitionBuilderTest {
    @Test
    void gatheringBuilderUsesSafeDefaultsAndSupportsReplanting() {
        ReplantDefinition replant = new ReplantDefinition(
                ContentSelector.id("example:seed"), "age", List.of(1, 2), 2, 10);

        GatheringContentDefinition definition = GatheringContentDefinition.builder(
                        NamespacedId.parse("example:crop"),
                        NamespacedId.parse("fabricmmo:herbalism"),
                        ContentSelector.tag("example:crops"),
                        25)
                .validTools(ContentSelector.tag("minecraft:hoes"))
                .naturalBlocksOnly(false)
                .maturity(MaturityRequirement.maximum("age"))
                .bonusDrops(true)
                .activeAbility(true)
                .replant(replant)
                .metadata(Map.of("source_mod", "example"))
                .build();

        assertEquals(25, definition.xp());
        assertFalse(definition.naturalBlocksOnly());
        assertTrue(definition.bonusDrops());
        assertTrue(definition.activeAbility());
        assertEquals(replant, definition.replant().orElseThrow());
        assertEquals("example", definition.metadata().get("source_mod"));
    }

    @Test
    void brewingBuilderCopiesMetadata() {
        BrewingContentDefinition definition = BrewingContentDefinition.builder(
                        NamespacedId.parse("example:brew"),
                        ContentSelector.id("example:ingredient"),
                        ContentSelector.id("minecraft:water_bottle"),
                        ContentSelector.id("example:result"),
                        2)
                .metadata(Map.of("recipe", "example:brew"))
                .build();

        assertEquals(2, definition.stage());
        assertEquals("example:brew", definition.metadata().get("recipe"));
    }
}
