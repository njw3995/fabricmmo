package io.github.njw3995.fabricmmo.core.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegionPlacedBlockTrackerTest {
    @Test
    void storesManyChunksInRegionFilesAndFlushesOnUnload(@TempDir Path directory)
            throws Exception {
        Path legacy = directory.resolve("legacy");
        Path regions = directory.resolve("world").resolve("fabricmmo_regions");
        TrackedWorld world = new TrackedWorld("minecraft:overworld", regions, -64, 320);
        BlockLocation first = new BlockLocation(world.worldId(), 1, 64, 1);
        BlockLocation sameRegion = new BlockLocation(world.worldId(), (31 << 4) + 2, 70, 3);
        BlockLocation nextRegion = new BlockLocation(world.worldId(), 32 << 4, 80, 4);

        try (RegionPlacedBlockTracker tracker = new RegionPlacedBlockTracker(legacy)) {
            tracker.registerWorld(world);
            tracker.setIneligible(first);
            tracker.setIneligible(sameRegion);
            tracker.setIneligible(nextRegion);

            tracker.chunkUnloaded(world.worldId(), first.chunkX(), first.chunkZ());
            tracker.chunkUnloaded(
                    world.worldId(), sameRegion.chunkX(), sameRegion.chunkZ());
            tracker.chunkUnloaded(
                    world.worldId(), nextRegion.chunkX(), nextRegion.chunkZ());
        }

        try (var files = Files.list(regions)) {
            assertEquals(2L, files.filter(path -> path.toString().endsWith(".fmr")).count());
        }

        try (RegionPlacedBlockTracker tracker = new RegionPlacedBlockTracker(legacy)) {
            tracker.registerWorld(world);
            assertTrue(tracker.isIneligible(first));
            assertTrue(tracker.isIneligible(sameRegion));
            assertTrue(tracker.isIneligible(nextRegion));
            tracker.setEligible(first);
        }

        try (RegionPlacedBlockTracker tracker = new RegionPlacedBlockTracker(legacy)) {
            tracker.registerWorld(world);
            assertFalse(tracker.isIneligible(first));
            assertTrue(tracker.isIneligible(sameRegion));
        }
    }

    @Test
    void migratesLegacyPerChunkFilesWithoutLosingMarkers(@TempDir Path directory)
            throws Exception {
        String worldId = "minecraft:overworld";
        BlockLocation location = new BlockLocation(worldId, -17, 42, 31);
        Path legacyRoot = directory.resolve("placed-blocks");
        String encodedWorld = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(worldId.getBytes(StandardCharsets.UTF_8));
        Path legacyFile = legacyRoot.resolve(encodedWorld)
                .resolve("c." + location.chunkX() + '.' + location.chunkZ() + ".fmpb");
        Files.createDirectories(legacyFile.getParent());
        try (DataOutputStream output = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(legacyFile)))) {
            output.writeInt(0x464D5042);
            output.writeInt(1);
            output.writeUTF(worldId);
            output.writeInt(location.chunkX());
            output.writeInt(location.chunkZ());
            output.writeInt(1);
            output.writeInt(location.x());
            output.writeInt(location.y());
            output.writeInt(location.z());
        }

        TrackedWorld world = new TrackedWorld(
                worldId, directory.resolve("world").resolve("fabricmmo_regions"), -64, 320);
        try (RegionPlacedBlockTracker tracker = new RegionPlacedBlockTracker(legacyRoot)) {
            tracker.registerWorld(world);
            assertTrue(tracker.isIneligible(location));
            assertFalse(Files.exists(legacyFile));
        }

        try (RegionPlacedBlockTracker tracker = new RegionPlacedBlockTracker(legacyRoot)) {
            tracker.registerWorld(world);
            assertTrue(tracker.isIneligible(location));
        }
    }

    @Test
    void preservesMcMmoNegativeCoordinateMapping(@TempDir Path directory) throws Exception {
        TrackedWorld world = new TrackedWorld(
                "minecraft:overworld", directory.resolve("regions"), -64, 320);
        BlockLocation negative = new BlockLocation(world.worldId(), -1, 64, -1);
        BlockLocation positiveLocalMirror = new BlockLocation(world.worldId(), -15, 64, -15);

        try (RegionPlacedBlockTracker tracker =
                     new RegionPlacedBlockTracker(directory.resolve("legacy"))) {
            tracker.registerWorld(world);
            tracker.setIneligible(negative);
            assertFalse(tracker.isIneligible(positiveLocalMirror));
        }
    }
}
