package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.ui.UiSettings;
import java.util.ArrayList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PlayerUtilityCommands {
    private PlayerUtilityCommands() { }

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        dispatcher.register(CommandManager.literal("mcability")
                .requires(source -> permissions.hasPermission(
                        source, PermissionNodes.MCABILITY, true))
                .executes(context -> toggleAbility(context.getSource()))
                .then(CommandManager.literal("?")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "mcability", context.getSource())))
                .then(CommandManager.literal("help")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "mcability", context.getSource())))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, PermissionNodes.MCABILITY_OTHERS, 2))
                        .suggests(SharedCommandUtil::suggestPlayers)
                        .executes(context -> toggleAbilityOther(
                                context.getSource(),
                                StringArgumentType.getString(context, "player")))));

        var notify = dispatcher.register(CommandManager.literal("mcnotify")
                .requires(source -> permissions.hasPermission(
                        source, PermissionNodes.MCNOTIFY, true))
                .executes(context -> toggle(context.getSource(), Toggle.NOTIFY)));
        dispatcher.register(CommandManager.literal("notify").redirect(notify));

        var sound = dispatcher.register(CommandManager.literal("mclevelupsound")
                .requires(source -> permissions.hasPermission(
                        source, PermissionNodes.MCLEVELUPSOUND, true))
                .executes(context -> toggle(context.getSource(), Toggle.SOUND)));
        dispatcher.register(CommandManager.literal("levelupsound").redirect(sound));

        dispatcher.register(CommandManager.literal("mcrefresh")
                .requires(source -> permissions.hasPermission(
                        source, PermissionNodes.MCREFRESH, 2))
                .executes(context -> refresh(context.getSource(), player(context.getSource())))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, PermissionNodes.MCREFRESH_OTHERS, 2))
                        .suggests(SharedCommandUtil::suggestPlayers)
                        .executes(context -> refreshOther(
                                context.getSource(),
                                StringArgumentType.getString(context, "player")))));

        var cooldown = dispatcher.register(CommandManager.literal("mccooldown")
                .requires(source -> permissions.hasPermission(
                        source, PermissionNodes.MCCOOLDOWN, true))
                .executes(context -> cooldowns(context.getSource())));
        dispatcher.register(CommandManager.literal("mccooldowns").redirect(cooldown));

        var power = dispatcher.register(CommandManager.literal("mmopower")
                .requires(source -> permissions.hasPermission(
                        source, PermissionNodes.MMOPOWER, true))
                .executes(context -> power(context.getSource())));
        dispatcher.register(CommandManager.literal("mmopowerlevel").redirect(power));
        dispatcher.register(CommandManager.literal("powerlevel").redirect(power));

        dispatcher.register(CommandManager.literal("mcgod")
                .requires(source -> permissions.hasPermission(
                        source, "mcmmo.commands.mcgod", 2))
                .executes(context -> toggle(context.getSource(), Toggle.GOD)));

        // Upstream intentionally leaves /mmodebug permissionless so players can
        // collect support diagnostics without an administrator granting access.
        var debug = dispatcher.register(CommandManager.literal("mmodebug")
                .executes(context -> toggle(context.getSource(), Toggle.DEBUG))
                .then(CommandManager.literal("?")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "mmodebug", context.getSource())))
                .then(CommandManager.literal("help")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "mmodebug", context.getSource()))));
        dispatcher.register(CommandManager.literal("mcmmodebugmode").redirect(debug));
    }

    private static ServerPlayerEntity player(ServerCommandSource source) {
        try {
            return source.getPlayerOrThrow();
        } catch (Exception exception) {
            source.sendError(Text.literal("This command requires a player."));
            return null;
        }
    }

    private static int toggleAbility(ServerCommandSource source) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        boolean enabled = SharedServerSystems.require().sessions()
                .toggleAbility(player.getUuid()).abilityUse();
        return SharedCommandUtil.success(
                source, "Ability use " + (enabled ? "enabled" : "disabled") + ".");
    }

    private static int toggleAbilityOther(ServerCommandSource source, String name) {
        var id = SharedCommandUtil.playerId(source, name);
        if (id.isEmpty()) return SharedCommandUtil.error(source, "Player not found: " + name);
        boolean enabled = SharedServerSystems.require().sessions()
                .toggleAbility(id.orElseThrow()).abilityUse();
        return SharedCommandUtil.success(source,
                "Ability use for " + name + ' '
                        + (enabled ? "enabled" : "disabled") + ".");
    }

    private static int toggle(ServerCommandSource source, Toggle toggle) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        var sessions = SharedServerSystems.require().sessions();
        boolean enabled = switch (toggle) {
            case NOTIFY -> sessions.toggleNotifications(player.getUuid()).notifications();
            case SOUND -> sessions.toggleLevelUpSound(player.getUuid()).levelUpSound();
            case DEBUG -> sessions.toggleDebug(player.getUuid()).debug();
            case GOD -> sessions.toggleGodMode(player.getUuid()).godMode();
        };
        int result = SharedCommandUtil.success(source,
                toggle.label + ' ' + (enabled ? "enabled" : "disabled") + ".");
        if (toggle == Toggle.DEBUG && enabled) {
            var systems = SharedServerSystems.require();
            systems.diagnostics().snapshot(player, systems.api(), systems.cooldowns());
        }
        return result;
    }

    private static int refresh(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) return 0;
        SharedServerSystems.require().cooldowns().reset(target.getUuid());
        target.sendMessage(Text.literal("Your abilities are refreshed."));
        return 1;
    }

    private static int refreshOther(ServerCommandSource source, String name) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(name);
        if (target == null) {
            return SharedCommandUtil.error(source, "Player must be online: " + name);
        }
        refresh(source, target);
        return SharedCommandUtil.success(source, "Refreshed abilities for " + name + ".");
    }

    private static int cooldowns(ServerCommandSource source) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        ArrayList<Text> lines = new ArrayList<>();
        var remaining = SharedServerSystems.require().cooldowns().remaining(player.getUuid());
        boolean abilityNames = SharedServerSystems.require().uiConfiguration().abilityNames();
        remaining.forEach((id, seconds) -> lines.add(Text.literal(
                        cooldownLabel(id.path(), abilityNames) + ": "
                                + (seconds == 0 ? "Ready" : seconds + "s"))
                .formatted(seconds == 0 ? Formatting.GREEN : Formatting.YELLOW)));
        if (lines.isEmpty()) {
            lines.add(Text.literal("No cooldown providers are registered.")
                    .formatted(Formatting.GRAY));
        }
        CommandUiDisplay.configured(
                source,
                UiSettings.BoardType.COOLDOWN,
                Text.literal("Ability Cooldowns"),
                Text.literal("---- Ability Cooldowns ----").formatted(Formatting.GOLD),
                lines);
        return 1;
    }

    private static String cooldownLabel(String abilityPath, boolean abilityNames) {
        if (abilityNames) return SharedCommandUtil.cap(abilityPath.replace('_', ' '));
        return switch (abilityPath) {
            case "super_breaker", "blast_mining" -> "Mining";
            default -> SharedCommandUtil.cap(abilityPath.replace('_', ' '));
        };
    }

    private static int power(ServerCommandSource source) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        int power = SharedCommandUtil.api().progression().queryAll(player.getUuid()).entrySet()
                .stream()
                .filter(entry -> SharedCommandUtil.api().skillRegistry().find(entry.getKey())
                        .map(skill -> !skill.childSkill()).orElse(false))
                .mapToInt(entry -> entry.getValue().level())
                .sum();
        source.sendMessage(Text.literal("Power Level: ").formatted(Formatting.GOLD)
                .append(Text.literal(Integer.toString(power)).formatted(Formatting.GREEN)));
        return 1;
    }

    private enum Toggle {
        NOTIFY("Notifications"),
        SOUND("Level-up sound"),
        DEBUG("Debug mode"),
        GOD("God mode");

        private final String label;
        Toggle(String label) { this.label = label; }
    }
}
