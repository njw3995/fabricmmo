package io.github.njw3995.fabricmmo.core.block;

import java.io.IOException;

public interface PlacedBlockTracker extends AutoCloseable {
    boolean isPlaced(BlockLocation location) throws IOException;

    void markPlaced(BlockLocation location) throws IOException;

    void clear(BlockLocation location) throws IOException;

    @Override
    void close() throws IOException;
}
