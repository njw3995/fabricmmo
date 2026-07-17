package io.github.njw3995.fabricmmo.core.command;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.command.CommandMetadata;
import io.github.njw3995.fabricmmo.api.command.CommandMetadataRegistrar;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.util.List;

public final class CoreCommandMetadata {
    private CoreCommandMetadata() {
    }

    public static void registerDefaults(CommandMetadataRegistrar registrar) {
        registrar.registerCommandMetadata(new CommandMetadata(
                NamespacedId.parse("fabricmmo:mcmmo"),
                "mcmmo",
                List.of(),
                PermissionNodes.MCMMO_DESCRIPTION));
        registrar.registerCommandMetadata(new CommandMetadata(
                NamespacedId.parse("fabricmmo:mcstats"),
                "mcstats",
                List.of("stats"),
                PermissionNodes.MCSTATS));
        registrar.registerCommandMetadata(new CommandMetadata(
                NamespacedId.parse("fabricmmo:addxp"),
                "addxp",
                List.of(),
                PermissionNodes.ADD_XP));
    }
}
