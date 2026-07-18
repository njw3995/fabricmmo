package io.github.njw3995.fabricmmo.core.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpstreamPermissionCatalogTest {
    @Test
    void loadsCompletePinnedPermissionTree() {
        UpstreamPermissionCatalog catalog = UpstreamPermissionCatalog.instance();

        assertEquals(593, catalog.definitions().size());
        assertTrue(catalog.find("mcmmo.commands.mining").isPresent());
        assertTrue(catalog.find("mcmmo.commands.defaultsop").isPresent());
    }

    @Test
    void appliesUpstreamPlayerAndOperatorDefaults() {
        UpstreamPermissionCatalog catalog = UpstreamPermissionCatalog.instance();

        assertTrue(catalog.effectiveDefault("mcmmo.commands.mining", false));
        assertFalse(catalog.effectiveDefault("mcmmo.commands.addxp", false));
        assertTrue(catalog.effectiveDefault("mcmmo.commands.addxp", true));
    }

    @Test
    void preservesAllParentImplications() {
        UpstreamPermissionCatalog catalog = UpstreamPermissionCatalog.instance();

        assertTrue(catalog.ancestors("mcmmo.commands.skillreset.mining")
                .contains("mcmmo.commands.skillreset.all"));
        assertTrue(catalog.ancestors("mcmmo.commands.skillreset.mining")
                .contains("mcmmo.defaultsop"));
    }
}
