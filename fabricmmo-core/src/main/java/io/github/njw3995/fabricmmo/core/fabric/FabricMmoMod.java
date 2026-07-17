package io.github.njw3995.fabricmmo.core.fabric;

import io.github.njw3995.fabricmmo.core.command.FabricMmoCommands;
import io.github.njw3995.fabricmmo.core.config.DefaultConfigInstaller;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricMmoMod implements ModInitializer {
    @Override
    public void onInitialize() {
        installDefaultConfigs();
        FabricCommandPermissionService permissions = new FabricCommandPermissionService();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                FabricMmoCommands.register(dispatcher, registryAccess, environment, permissions));
        ServerLifecycleEvents.SERVER_STARTING.register(FabricMmoFabricRuntime::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(ignored -> FabricMmoFabricRuntime.stop());
    }

    private static void installDefaultConfigs() {
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("fabricmmo");
        try {
            DefaultConfigInstaller.installMissingDefaults(configDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to install FabricMMO configuration", exception);
        }
    }
}
