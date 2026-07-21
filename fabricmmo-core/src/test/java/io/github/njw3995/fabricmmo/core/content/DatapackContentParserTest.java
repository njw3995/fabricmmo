package io.github.njw3995.fabricmmo.core.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import java.io.StringReader;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class DatapackContentParserTest {
    @Test
    void parsesCompleteGatheringDefinitionFromCanonicalResourceId() {
        String json = """
                {
                  "format": 1,
                  "skill": "fabricmmo:herbalism",
                  "block": "#example:crops",
                  "xp": 42,
                  "valid_tools": ["#minecraft:hoes"],
                  "natural_blocks_only": false,
                  "maturity": {"mode": "maximum", "property": "age"},
                  "bonus_drops": true,
                  "active_ability": true,
                  "replant": {
                    "planting_item": "example:seed",
                    "age_property": "age",
                    "rank_ages": [0, 1, 2, 3, 4],
                    "active_ability_rank_bonus": 1,
                    "delay_ticks": 2
                  },
                  "metadata": {"source": "test"}
                }
                """;

        var parsed = DatapackContentParser.gathering(
                Identifier.of("example", "fabricmmo/gathering/crop.json"),
                new StringReader(json));
        var definition = parsed.definition().orElseThrow();

        assertEquals(NamespacedId.parse("example:crop"), definition.id());
        assertEquals(ContentSelector.tag("example:crops"), definition.block());
        assertEquals(42, definition.xp());
        assertFalse(definition.naturalBlocksOnly());
        assertTrue(definition.bonusDrops());
        assertTrue(definition.activeAbility());
        assertEquals(2, definition.replant().orElseThrow().delayTicks());
    }

    @Test
    void parsesDisableOverlayAndBrewingRecipe() {
        var disabled = DatapackContentParser.gathering(
                Identifier.of("example", "fabricmmo/gathering/ore.json"),
                new StringReader("{\"format\":1,\"enabled\":false}"));
        assertEquals(NamespacedId.parse("example:ore"), disabled.id());
        assertTrue(disabled.definition().isEmpty());

        var brewing = DatapackContentParser.brewing(
                Identifier.of("example", "fabricmmo/brewing/tea.json"),
                new StringReader("""
                        {
                          "format": 1,
                          "ingredient": "example:leaf",
                          "input": "minecraft:potion",
                          "output": "example:tea",
                          "stage": 2
                        }
                        """));
        assertEquals(2, brewing.definition().orElseThrow().stage());
    }


    @Test
    void parsesEntityXpDefinitionsAndDisableOverlays() {
        var combat = DatapackContentParser.entityXp(
                Identifier.of("example", "fabricmmo/entity_xp/crystal_golem.json"),
                new StringReader("""
                        {
                          "format": 1,
                          "scope": "combat",
                          "entity": "#example:crystal_golems",
                          "xp": 2.5,
                          "metadata": {"source": "example"}
                        }
                        """));
        var definition = combat.definition().orElseThrow();
        assertEquals(io.github.njw3995.fabricmmo.api.content.EntityXpContentDefinition.Scope.COMBAT,
                definition.scope());
        assertEquals(2.5D, definition.xp());
        assertEquals(ContentSelector.tag("example:crystal_golems"), definition.entity());

        var disabled = DatapackContentParser.entityXp(
                Identifier.of("example", "fabricmmo/entity_xp/crystal_golem.json"),
                new StringReader("{\"format\":1,\"enabled\":false}"));
        assertTrue(disabled.definition().isEmpty());
    }

    @Test
    void rejectsUnknownKeysVersionsAndMalformedMaturity() {
        Identifier id = Identifier.of("example", "fabricmmo/gathering/bad.json");
        assertThrows(IllegalArgumentException.class, () -> DatapackContentParser.gathering(
                id, new StringReader("{\"format\":2}")));
        assertThrows(IllegalArgumentException.class, () -> DatapackContentParser.gathering(
                id, new StringReader("{\"enabled\":false,\"typo\":true}")));
        assertThrows(IllegalArgumentException.class, () -> DatapackContentParser.gathering(
                id, new StringReader("""
                        {
                          "skill":"fabricmmo:herbalism",
                          "block":"example:crop",
                          "xp":1,
                          "maturity":{"mode":"at_least","property":"age"}
                        }
                        """)));
        assertThrows(IllegalArgumentException.class, () -> DatapackContentParser.gathering(
                id, new StringReader("""
                        {
                          "skill":"fabricmmo:herbalism",
                          "block":"example:crop",
                          "xp":1,
                          "replant":{
                            "planting_item":"example:seed",
                            "age_property":"age",
                            "rank_ages":[0,1.5]
                          }
                        }
                        """)));
        assertThrows(IllegalArgumentException.class, () -> DatapackContentParser.entityXp(
                Identifier.of("example", "fabricmmo/entity_xp/bad.json"),
                new StringReader("{\"scope\":\"unknown\",\"entity\":\"example:mob\",\"xp\":1}")));
    }
}
