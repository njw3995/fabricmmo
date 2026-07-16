package io.github.njw3995.fabricmmo.core.fabric;

import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import net.fabricmc.api.ModInitializer;

public final class FabricMmoMod implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
    }
}
