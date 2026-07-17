package io.github.njw3995.fabricmmo.core.block;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;

public record PlacedBlockSettings(boolean regionSystemEnabled) {
    public static PlacedBlockSettings load(Path persistentDataFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(persistentDataFile);
        return new PlacedBlockSettings(config.bool("mcMMO_Region_System.Enabled", true));
    }
}
