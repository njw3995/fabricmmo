package io.github.njw3995.fabricmmo.core.skill.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class RepairSalvageDefaultsTest {
    private static final Path DEFAULTS = Path.of("src/main/resources/defaults");

    @Test
    void completeUpstreamTablesArePackaged() throws Exception {
        RepairDefinitionTable repair = RepairDefinitionTable.load(
                DEFAULTS.resolve("repair.vanilla.yml"));
        SalvageDefinitionTable salvage = SalvageDefinitionTable.load(
                DEFAULTS.resolve("salvage.vanilla.yml"));
        assertTrue(repair.entries().size() >= 60, "repair table was reduced");
        assertTrue(salvage.entries().size() >= 60, "salvage table was reduced");
    }

    @Test
    void criticalTridentAndNetheriteDefaultsMatchUpstream() throws Exception {
        RepairDefinitionTable repair = RepairDefinitionTable.load(
                DEFAULTS.resolve("repair.vanilla.yml"));
        SalvageDefinitionTable salvage = SalvageDefinitionTable.load(
                DEFAULTS.resolve("salvage.vanilla.yml"));
        assertEquals("PRISMARINE_CRYSTALS",
                salvage.find("TRIDENT").orElseThrow().salvageMaterialName());
        assertEquals(16, salvage.find("TRIDENT").orElseThrow().maximumQuantity());
        assertEquals("NETHERITE_SCRAP",
                repair.find("NETHERITE_PICKAXE").orElseThrow().repairMaterialName());
        assertEquals(4,
                repair.find("NETHERITE_PICKAXE").orElseThrow().minimumQuantity());
        assertEquals("NETHERITE_SCRAP",
                salvage.find("NETHERITE_PICKAXE").orElseThrow().salvageMaterialName());
        assertEquals(4,
                salvage.find("NETHERITE_PICKAXE").orElseThrow().maximumQuantity());
    }
    @Test
    void packagedMappingsAreByteExactPinnedUpstreamResources() throws Exception {
        assertEquals("51c0cde6c5384f4584cfa57eba98409025303378fdabee5445864cabfa1524ee",
                sha256(DEFAULTS.resolve("repair.vanilla.yml")));
        assertEquals("9306fc74e882a99c191deeb9a56c1d72cc141132456a427ecdb0cd3647b96270",
                sha256(DEFAULTS.resolve("salvage.vanilla.yml")));
        assertEquals("e3534c5dd48065a57f300ed6be31ea0941a866cc88de000694a6f636d8ae3482",
                sha256(DEFAULTS.resolve("custom_item_support.yml")));
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

}
