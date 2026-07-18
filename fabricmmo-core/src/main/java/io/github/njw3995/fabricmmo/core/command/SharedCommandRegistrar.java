package io.github.njw3995.fabricmmo.core.command;
import com.mojang.brigadier.CommandDispatcher;import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;import net.minecraft.server.command.ServerCommandSource;
public final class SharedCommandRegistrar{private SharedCommandRegistrar(){}public static void register(CommandDispatcher<ServerCommandSource>d,CommandPermissionService p){InformationCommands.register(d,p);LeaderboardCommands.register(d,p);XpRateCommands.register(d,p);PartyCommands.register(d,p);UiCommands.register(d,p);MaintenanceCommands.register(d,p);}}
