package io.github.njw3995.fabricmmo.core.permission;

import net.minecraft.server.command.ServerCommandSource;

public interface CommandPermissionService {
    boolean hasPermission(ServerCommandSource source, String permission, boolean fallback);

    boolean hasPermission(ServerCommandSource source, String permission, int fallbackLevel);
}
