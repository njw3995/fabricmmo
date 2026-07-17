package io.github.njw3995.fabricmmo.core.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChunkPlacedBlockTrackerTest {
    @Test
    void persistsMarkersAcrossRestartAndClearsThem(@TempDir Path directory) throws Exception {
        BlockLocation location = new BlockLocation("minecraft:overworld", -17, 64, 31);

        try (ChunkPlacedBlockTracker tracker = new ChunkPlacedBlockTracker(directory)) {
            assertFalse(tracker.isPlaced(location));
            tracker.markPlaced(location);
            assertTrue(tracker.isPlaced(location));
        }

        try (ChunkPlacedBlockTracker tracker = new ChunkPlacedBlockTracker(directory)) {
            assertTrue(tracker.isPlaced(location));
            tracker.clear(location);
            assertFalse(tracker.isPlaced(location));
        }

        try (ChunkPlacedBlockTracker tracker = new ChunkPlacedBlockTracker(directory)) {
            assertFalse(tracker.isPlaced(location));
        }
    }

    @Test
    void isolatesDimensionsAndChunks(@TempDir Path directory) throws Exception {
        BlockLocation overworld = new BlockLocation("minecraft:overworld", 15, 70, 15);
        BlockLocation nextChunk = new BlockLocation("minecraft:overworld", 16, 70, 15);
        BlockLocation nether = new BlockLocation("minecraft:the_nether", 15, 70, 15);

        try (ChunkPlacedBlockTracker tracker = new ChunkPlacedBlockTracker(directory)) {
            tracker.markPlaced(overworld);
            assertTrue(tracker.isPlaced(overworld));
            assertFalse(tracker.isPlaced(nextChunk));
            assertFalse(tracker.isPlaced(nether));
        }
    }
}
