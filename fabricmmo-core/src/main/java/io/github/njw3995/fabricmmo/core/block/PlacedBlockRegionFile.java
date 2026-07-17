package io.github.njw3995.fabricmmo.core.block;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;

/**
 * Compact 32x32-chunk region container. The layout mirrors Minecraft's sector-based region
 * approach: one 4 KiB location table followed by variable-sized chunk payloads.
 */
final class PlacedBlockRegionFile implements Closeable {
    private static final int SECTOR_BYTES = 4096;
    private static final int HEADER_SECTORS = 1;
    private static final int HEADER_BYTES = SECTOR_BYTES * HEADER_SECTORS;
    private static final int CHUNKS_PER_AXIS = 32;
    private static final int ENTRY_COUNT = CHUNKS_PER_AXIS * CHUNKS_PER_AXIS;

    private final RandomAccessFile file;
    private final int[] locations = new int[ENTRY_COUNT];
    private final BitSet usedSectors = new BitSet();

    PlacedBlockRegionFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        file = new RandomAccessFile(path.toFile(), "rw");
        if (file.length() < HEADER_BYTES) {
            file.setLength(HEADER_BYTES);
        }
        file.seek(0L);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            locations[index] = file.readInt();
        }
        usedSectors.set(0, HEADER_SECTORS);
        int totalSectors = sectorCount(file.length());
        for (int location : locations) {
            int offset = location >>> 8;
            int count = location & 0xFF;
            if (offset < HEADER_SECTORS || count <= 0 || offset + count > totalSectors) {
                continue;
            }
            usedSectors.set(offset, offset + count);
        }
    }

    byte[] read(int chunkX, int chunkZ) throws IOException {
        int location = locations[index(chunkX, chunkZ)];
        int offset = location >>> 8;
        int count = location & 0xFF;
        if (offset == 0 || count == 0) {
            return null;
        }
        if (offset < HEADER_SECTORS || offset + count > sectorCount(file.length())) {
            throw new IOException("Placed-block region location points outside the file");
        }
        file.seek((long) offset * SECTOR_BYTES);
        int length;
        try {
            length = file.readInt();
        } catch (EOFException exception) {
            throw new IOException("Truncated placed-block region entry", exception);
        }
        int maximumLength = count * SECTOR_BYTES - Integer.BYTES;
        if (length < 0 || length > maximumLength) {
            throw new IOException("Invalid placed-block region entry length: " + length);
        }
        byte[] bytes = new byte[length];
        file.readFully(bytes);
        return bytes;
    }

    void write(int chunkX, int chunkZ, byte[] payload) throws IOException {
        int entryIndex = index(chunkX, chunkZ);
        int oldLocation = locations[entryIndex];
        int oldOffset = oldLocation >>> 8;
        int oldCount = oldLocation & 0xFF;

        if (payload == null || payload.length == 0) {
            free(oldOffset, oldCount);
            updateLocation(entryIndex, 0);
            return;
        }

        int needed = sectorCount(Integer.BYTES + payload.length);
        if (needed > 0xFF) {
            throw new IOException("Placed-block chunk payload exceeds region entry limit");
        }

        int offset;
        if (oldOffset >= HEADER_SECTORS && oldCount >= needed) {
            offset = oldOffset;
            if (oldCount > needed) {
                usedSectors.clear(oldOffset + needed, oldOffset + oldCount);
            }
        } else {
            free(oldOffset, oldCount);
            offset = findFreeRun(needed);
            if (offset < 0) {
                offset = Math.max(HEADER_SECTORS, sectorCount(file.length()));
                file.setLength((long) (offset + needed) * SECTOR_BYTES);
            }
            usedSectors.set(offset, offset + needed);
        }

        file.seek((long) offset * SECTOR_BYTES);
        file.writeInt(payload.length);
        file.write(payload);
        int padding = needed * SECTOR_BYTES - Integer.BYTES - payload.length;
        if (padding > 0) {
            file.write(new byte[padding]);
        }
        updateLocation(entryIndex, (offset << 8) | needed);
    }

    private int findFreeRun(int needed) {
        int candidate = HEADER_SECTORS;
        int total = Math.max(HEADER_SECTORS, sectorCount(fileLengthUnchecked()));
        while (candidate + needed <= total) {
            candidate = usedSectors.nextClearBit(candidate);
            int nextUsed = usedSectors.nextSetBit(candidate);
            if (nextUsed < 0 || nextUsed - candidate >= needed) {
                return candidate;
            }
            candidate = nextUsed + 1;
        }
        return -1;
    }

    private long fileLengthUnchecked() {
        try {
            return file.length();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect region file length", exception);
        }
    }

    private void free(int offset, int count) {
        if (offset >= HEADER_SECTORS && count > 0) {
            usedSectors.clear(offset, offset + count);
        }
    }

    private void updateLocation(int entryIndex, int location) throws IOException {
        locations[entryIndex] = location;
        file.seek((long) entryIndex * Integer.BYTES);
        file.writeInt(location);
    }

    private static int index(int chunkX, int chunkZ) {
        return (chunkX & 31) + ((chunkZ & 31) * CHUNKS_PER_AXIS);
    }

    private static int sectorCount(long byteCount) {
        return (int) ((byteCount + SECTOR_BYTES - 1L) / SECTOR_BYTES);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
