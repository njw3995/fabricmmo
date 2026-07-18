package io.github.njw3995.fabricmmo.core.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.command.ServerCommandSource;

/** Fabric Permissions API bridge with mcMMO 2.3.000 parent/default semantics. */
public final class FabricCommandPermissionService implements CommandPermissionService {
    private static final int OP_FALLBACK_LEVEL = 2;
    private static final UpstreamPermissionCatalog CATALOG = UpstreamPermissionCatalog.instance();

    @Override
    public boolean hasPermission(ServerCommandSource source, String permission, boolean fallback) {
        return resolve(source, permission, fallback);
    }

    @Override
    public boolean hasPermission(ServerCommandSource source, String permission, int fallbackLevel) {
        boolean fallback = source.hasPermissionLevel(fallbackLevel);
        return resolve(source, permission, fallback);
    }

    private static boolean resolve(
            ServerCommandSource source,
            String permission,
            boolean unknownPermissionFallback) {
        TriState direct = Permissions.getPermissionValue(source, permission);
        if (direct != TriState.DEFAULT) {
            return direct == TriState.TRUE;
        }

        for (String parent : CATALOG.ancestors(permission)) {
            TriState inherited = Permissions.getPermissionValue(source, parent);
            if (inherited != TriState.DEFAULT) {
                return inherited == TriState.TRUE;
            }
        }

        return CATALOG.find(permission).isPresent()
                ? CATALOG.effectiveDefault(permission, source.hasPermissionLevel(OP_FALLBACK_LEVEL))
                : unknownPermissionFallback;
    }
}
