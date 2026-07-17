package io.github.njw3995.fabricmmo.core.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

public final class FabricCommandPermissionService implements CommandPermissionService {
    @Override
    public boolean hasPermission(ServerCommandSource source, String permission, boolean fallback) {
        return Permissions.check(source, permission, fallback);
    }

    @Override
    public boolean hasPermission(ServerCommandSource source, String permission, int fallbackLevel) {
        return Permissions.check(source, permission, fallbackLevel);
    }
}
