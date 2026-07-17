package io.github.njw3995.fabricmmo.core.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldBlacklistTest {
    @TempDir
    Path tempDirectory;

    @Test
    void matchesUpstreamCaseInsensitivelyWithoutTrimmingOrComments() throws Exception {
        Path blacklist = tempDirectory.resolve("world_blacklist.txt");
        Files.writeString(blacklist, "world\nWORLD_NETHER\n world_the_end\n#comment\n");
        WorldBlacklist loaded = WorldBlacklist.load(blacklist, tempDirectory.resolve("world"));

        assertTrue(loaded.isBlacklisted("WoRlD"));
        assertTrue(loaded.isBlacklisted("world_nether"));
        assertFalse(loaded.isBlacklisted("world_the_end"));
        assertTrue(loaded.isBlacklisted(" world_the_end"));
        assertTrue(loaded.isBlacklisted("#COMMENT"));
    }
}
