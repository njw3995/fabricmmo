package io.github.njw3995.fabricmmo.core.block;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Objects;

/** Compact 16x16xworld-height ineligibility store matching mcMMO's bitset layout. */
final class BitSetChunkStore {
    private static final int MAGIC = 0x464D4342; // FMCB
    private static final int VERSION = 1;

    private final String worldId;
    private final int chunkX;
    private final int chunkZ;
    private final int minimumY;
    private final int maximumYExclusive;
    private final BitSet bits;
    private boolean dirty;

    BitSetChunkStore(TrackedWorld world, int chunkX, int chunkZ) {
        this(world.worldId(), chunkX, chunkZ, world.minimumY(), world.maximumYExclusive(),
                new BitSet(), false);
    }

    private BitSetChunkStore(
            String worldId,
            int chunkX,
            int chunkZ,
            int minimumY,
            int maximumYExclusive,
            BitSet bits,
            boolean dirty) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minimumY = minimumY;
        this.maximumYExclusive = maximumYExclusive;
        this.bits = Objects.requireNonNull(bits, "bits");
        this.dirty = dirty;
    }

    boolean isIneligible(int worldX, int y, int worldZ) {
        return bits.get(index(worldX, y, worldZ));
    }

    void set(int worldX, int y, int worldZ, boolean value) {
        int index = index(worldX, y, worldZ);
        if (bits.get(index) == value) {
            return;
        }
        bits.set(index, value);
        dirty = true;
    }

    boolean isDirty() {
        return dirty;
    }

    void markClean() {
        dirty = false;
    }

    boolean isEmpty() {
        return bits.isEmpty();
    }

    String worldId() {
        return worldId;
    }

    int chunkX() {
        return chunkX;
    }

    int chunkZ() {
        return chunkZ;
    }

    byte[] serialize() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeUTF(worldId);
            output.writeInt(chunkX);
            output.writeInt(chunkZ);
            output.writeInt(minimumY);
            output.writeInt(maximumYExclusive);
            byte[] bitBytes = bits.toByteArray();
            output.writeInt(bitBytes.length);
            output.write(bitBytes);
        }
        return bytes.toByteArray();
    }

    static BitSetChunkStore deserialize(byte[] bytes, TrackedWorld currentWorld) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int magic = input.readInt();
            int version = input.readInt();
            if (magic != MAGIC || version != VERSION) {
                throw new IOException("Unsupported placed-block chunk format");
            }
            String worldId = input.readUTF();
            int chunkX = input.readInt();
            int chunkZ = input.readInt();
            int storedMinimumY = input.readInt();
            int storedMaximumYExclusive = input.readInt();
            int byteCount = input.readInt();
            if (byteCount < 0 || byteCount > 16 * 1024 * 1024) {
                throw new IOException("Invalid placed-block bitset length: " + byteCount);
            }
            byte[] bitBytes = input.readNBytes(byteCount);
            if (bitBytes.length != byteCount || input.read() != -1) {
                throw new IOException("Truncated or trailing placed-block chunk data");
            }
            if (!worldId.equals(currentWorld.worldId())) {
                throw new IOException("Placed-block world mismatch: " + worldId);
            }

            BitSet stored = BitSet.valueOf(bitBytes);
            BitSet adjusted = adjustHeight(
                    stored,
                    storedMinimumY,
                    storedMaximumYExclusive,
                    currentWorld.minimumY(),
                    currentWorld.maximumYExclusive());
            boolean changedHeight = storedMinimumY != currentWorld.minimumY()
                    || storedMaximumYExclusive != currentWorld.maximumYExclusive();
            return new BitSetChunkStore(
                    worldId,
                    chunkX,
                    chunkZ,
                    currentWorld.minimumY(),
                    currentWorld.maximumYExclusive(),
                    adjusted,
                    changedHeight);
        }
    }

    private int index(int worldX, int y, int worldZ) {
        if (y < minimumY || y >= maximumYExclusive) {
            throw new IndexOutOfBoundsException("y outside world bounds: " + y);
        }
        // mcMMO has historically used Math.abs(coordinate) % 16. Preserve that mapping so
        // FabricMMO's tracker has the same negative-coordinate lookup behavior.
        int localX = Math.abs(worldX) % 16;
        int localZ = Math.abs(worldZ) % 16;
        return ((y - minimumY) * 256) + (localZ * 16) + localX;
    }

    private static BitSet adjustHeight(
            BitSet source,
            int oldMinimumY,
            int oldMaximumYExclusive,
            int newMinimumY,
            int newMaximumYExclusive) {
        if (oldMinimumY == newMinimumY && oldMaximumYExclusive == newMaximumYExclusive) {
            return source;
        }
        BitSet adjusted = new BitSet();
        int overlapMinimum = Math.max(oldMinimumY, newMinimumY);
        int overlapMaximum = Math.min(oldMaximumYExclusive, newMaximumYExclusive);
        for (int y = overlapMinimum; y < overlapMaximum; y++) {
            int oldOffset = (y - oldMinimumY) * 256;
            int newOffset = (y - newMinimumY) * 256;
            BitSet plane = source.get(oldOffset, oldOffset + 256);
            for (int bit = plane.nextSetBit(0); bit >= 0; bit = plane.nextSetBit(bit + 1)) {
                adjusted.set(newOffset + bit);
            }
        }
        return adjusted;
    }
}
