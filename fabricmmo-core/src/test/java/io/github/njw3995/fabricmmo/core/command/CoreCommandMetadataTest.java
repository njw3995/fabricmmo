package io.github.njw3995.fabricmmo.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import org.junit.jupiter.api.Test;

class CoreCommandMetadataTest {
    @Test
    void registersImplementedUpstreamCommandsAndAliases() {
        DefaultCommandMetadataRegistry registry = new DefaultCommandMetadataRegistry();

        CoreCommandMetadata.registerDefaults(registry);

        assertEquals(4, registry.commands().size());
        assertEquals("mcmmo.commands.mcstats",
                registry.find(NamespacedId.parse("fabricmmo:mcstats")).orElseThrow().permission());
        assertTrue(registry.find(NamespacedId.parse("fabricmmo:mcstats"))
                .orElseThrow().aliases().contains("stats"));
        assertEquals("mcmmo.commands.mining",
                registry.find(NamespacedId.parse("fabricmmo:mining")).orElseThrow().permission());
    }
}
