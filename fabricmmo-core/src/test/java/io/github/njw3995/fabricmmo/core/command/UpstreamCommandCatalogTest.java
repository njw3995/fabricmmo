package io.github.njw3995.fabricmmo.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpstreamCommandCatalogTest {
    @Test
    void loadsEveryPinnedUpstreamCommandAndAlias() {
        UpstreamCommandCatalog catalog = UpstreamCommandCatalog.instance();

        assertEquals(50, catalog.commands().size());
        assertEquals("mccooldown", catalog.find("mccooldowns").orElseThrow().literal());
        assertEquals(List.of("pc", "p"), catalog.find("partychat").orElseThrow().aliases());
        assertEquals("mcmmo.commands.mining",
                catalog.find("mining").orElseThrow().permission().orElseThrow());
        assertTrue(catalog.find("fabricmmo").isEmpty());
    }
}
