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
        register(registrar, "mcmmo", List.of("fabricmmo", "fmmo"),
                PermissionNodes.MCMMO_DESCRIPTION);
        register(registrar, "mcstats", List.of("stats"), PermissionNodes.MCSTATS);
        register(registrar, "mining", List.of(), PermissionNodes.MINING_COMMAND);
        register(registrar, "addxp", List.of(), PermissionNodes.ADD_XP);
        register(registrar, "mcability", List.of(), PermissionNodes.MCABILITY);
        register(registrar, "mccooldown", List.of("mccooldowns"), PermissionNodes.MCCOOLDOWN);
        register(registrar, "mclevelupsound", List.of("levelupsound"),
                PermissionNodes.MCLEVELUPSOUND);
        register(registrar, "mcnotify", List.of("notify"), PermissionNodes.MCNOTIFY);
        register(registrar, "mcrefresh", List.of(), PermissionNodes.MCREFRESH);
        register(registrar, "mmopower", List.of("mmopowerlevel", "powerlevel"),
                PermissionNodes.MMOPOWER);
        register(registrar, "addlevels", List.of(), PermissionNodes.ADD_LEVELS);
        register(registrar, "mmoedit", List.of(), PermissionNodes.MMO_EDIT);
        register(registrar, "skillreset", List.of(), PermissionNodes.SKILL_RESET);
    }

    private static void register(
            CommandMetadataRegistrar registrar,
            String literal,
            List<String> aliases,
            String permission) {
        registrar.registerCommandMetadata(new CommandMetadata(
                NamespacedId.parse("fabricmmo:" + literal),
                literal,
                aliases,
                permission));
    }
}
