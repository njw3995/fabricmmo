package io.github.njw3995.fabricmmo.core.fabric;

import io.github.njw3995.fabricmmo.core.command.FabricMmoCommands;
import io.github.njw3995.fabricmmo.core.chat.SharedChatHandler;
import io.github.njw3995.fabricmmo.core.config.DefaultConfigInstaller;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.axes.AxesAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismFoodHandler;
import io.github.njw3995.fabricmmo.core.skill.herbalism.HerbalismInteractionHandler;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingFoodHandler;
import io.github.njw3995.fabricmmo.core.skill.fishing.FishingRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlockBreakHandler;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.swords.SwordsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.unarmed.UnarmedAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.woodcutting.WoodcuttingBlockBreakHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricMmoMod implements ModInitializer {
    @Override
    public void onInitialize() {
        installDefaultConfigs();
        FabricCommandPermissionService permissions = new FabricCommandPermissionService();
        MiningBlockBreakHandler.register();
        MiningBlastHandler.register();
        MiningAbilityHandler.register();
        WoodcuttingBlockBreakHandler.register();
        WoodcuttingAbilityHandler.register();
        ExcavationBlockBreakHandler.register();
        ExcavationAbilityHandler.register();
        HerbalismBlockBreakHandler.register();
        HerbalismAbilityHandler.register();
        HerbalismInteractionHandler.register();
        HerbalismFoodHandler.register();
        FishingFoodHandler.register();
        SwordsAbilityHandler.register();
        AxesAbilityHandler.register();
        UnarmedAbilityHandler.register();
        SharedChatHandler.register(permissions);
        ServerTickEvents.END_SERVER_TICK.register(SharedServerSystems::tick);
        ServerTickEvents.END_SERVER_TICK.register(SwordsRuntimeHandler::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (SharedServerSystems.running()) {
                SharedServerSystems.playerJoined(handler.player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FishingRuntimeHandler.playerDisconnected(handler.player.getUuid());
            AcrobaticsRuntimeHandler.playerDisconnected(handler.player.getUuid());
            SwordsRuntimeHandler.playerDisconnected(handler.player.getUuid());
            SharedServerSystems.playerDisconnected(handler.player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            AcrobaticsRuntimeHandler.playerRespawned(newPlayer.getUuid());
            SwordsRuntimeHandler.playerRespawned(newPlayer.getUuid());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                boolean godMode = SharedServerSystems.godMode(player.getUuid());
                if (!godMode && SharedServerSystems.running()) {
                    SharedServerSystems.playerHurt(player);
                }
                return !godMode;
            }
            return true;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                FabricMmoCommands.register(dispatcher, registryAccess, environment, permissions));
        ServerLifecycleEvents.SERVER_STARTING.register(FabricMmoFabricRuntime::start);
        ServerWorldEvents.LOAD.register((server, world) -> FabricMmoFabricRuntime.worldLoaded(world));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
                FabricMmoFabricRuntime.chunkUnloaded(
                        world, chunk.getPos().x, chunk.getPos().z));
        ServerWorldEvents.UNLOAD.register((server, world) ->
                FabricMmoFabricRuntime.worldUnloaded(world));
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
