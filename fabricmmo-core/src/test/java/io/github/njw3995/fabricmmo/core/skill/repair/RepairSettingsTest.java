package io.github.njw3995.fabricmmo.core.skill.repair;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepairSettingsTest {
    private static final Path DEFAULTS = Path.of("src/main/resources/defaults");

    @Test
    void enchantedRepairMaterialsFollowPinnedUpstreamAdvancedConfigLookup(
            @TempDir Path temp) throws Exception {
        Path config = copy(temp, "config.yml");
        Path advanced = copy(temp, "advanced.yml");
        Path ranks = copy(temp, "skillranks.yml");
        Path experience = copy(temp, "experience.yml");
        Path custom = copy(temp, "custom_item_support.yml");

        String configText = Files.readString(config).replace(
                "Use_Enchanted_Materials: false", "Use_Enchanted_Materials: true");
        Files.writeString(config, configText);
        assertFalse(RepairSettings.load(config, advanced, ranks, experience, custom)
                .useEnchantedMaterials());

        Files.writeString(advanced, Files.readString(advanced)
                + "\nSkills:\n  Repair:\n    Use_Enchanted_Materials: true\n");
        assertTrue(RepairSettings.load(config, advanced, ranks, experience, custom)
                .useEnchantedMaterials());
    }

    private static Path copy(Path target, String name) throws Exception {
        return Files.copy(DEFAULTS.resolve(name), target.resolve(name));
    }
}
