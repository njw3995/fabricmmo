package io.github.njw3995.fabricmmo.core.skill.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlchemyPotionConfigTest {
    @TempDir Path temp;

    @Test
    void loadsCompletePinnedPotionGraphAndCumulativeTiers() throws Exception {
        AlchemyPotionConfig config = AlchemyPotionConfig.load(
                Path.of("src/main/resources/defaults/potions.yml"));
        assertEquals(232, config.potions().size());
        assertEquals(20, config.ingredientsForTier(1).size());
        assertEquals(32, config.ingredientsForTier(8).size());
        AlchemyPotionDefinition awkward = config.potion("POTION_OF_AWKWARD");
        assertNotNull(awkward);
        assertEquals("POTION_OF_HASTE", config.child(
                awkward, Identifier.of("minecraft", "carrot")).id());
        AlchemyPotionDefinition saturation = config.child(
                awkward, Identifier.of("minecraft", "fern"));
        assertEquals("POTION_OF_SATURATION", saturation.id());
        assertTrue(saturation.effects().stream().anyMatch(effect ->
                effect.effectId().equals(Identifier.of("minecraft", "saturation"))
                        && effect.duration() == 8));
    }

    @Test
    void unresolvedChildIsSkippedWithoutDiscardingValidConfiguration() throws Exception {
        Path file = temp.resolve("potions.yml");
        Files.writeString(file, """
                Concoctions:
                    Tier_One_Ingredients:
                        - NETHER_WART
                Potions:
                    POTION_OF_WATER:
                        Material: POTION
                        PotionData:
                            PotionType: WATER
                        Children:
                            NETHER_WART: MISSING_POTION
                """);
        AlchemyPotionConfig config = AlchemyPotionConfig.load(file);
        assertNotNull(config.potion("POTION_OF_WATER"));
        assertNull(config.child(config.potion("POTION_OF_WATER"),
                Identifier.of("minecraft", "nether_wart")));
    }

    @Test
    void malformedPotionDefinitionIsReportedAsCorruption() throws Exception {
        Path file = temp.resolve("malformed-potions.yml");
        Files.writeString(file, """
                Concoctions:
                    Tier_One_Ingredients:
                        - NETHER_WART
                Potions:
                    BROKEN:
                        Material: POTION
                """);
        assertThrows(IllegalArgumentException.class, () -> AlchemyPotionConfig.load(file));
    }
}
