package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.ui.UiSettings;
import io.github.njw3995.fabricmmo.core.ui.XpBarMode;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

final class UiCommands {
    private UiCommands() { }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        var xp = dispatcher.register(CommandManager.literal("mmoxpbar")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mmoxpbar", true))
                .executes(context -> usage(context.getSource()))
                .then(CommandManager.argument("setting", StringArgumentType.word())
                        .suggests((context, builder) -> net.minecraft.command.CommandSource
                                .suggestMatching(java.util.List.of("show", "hide", "reset", "disable"), builder))
                        .executes(context -> settingAll(
                                context.getSource(), StringArgumentType.getString(context, "setting")))
                        .then(CommandManager.argument("skill", StringArgumentType.word())
                                .suggests(SharedCommandUtil::suggestSkills)
                                .executes(context -> settingSkill(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "setting"),
                                        StringArgumentType.getString(context, "skill"))))));
        dispatcher.register(CommandManager.literal("xpbarsettings").redirect(xp));

        var scoreboard = dispatcher.register(CommandManager.literal("mcscoreboard")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mcscoreboard", true))
                .executes(context -> scoreboardHelp(context.getSource()))
                .then(CommandManager.literal("?")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "mcscoreboard", context.getSource())))
                .then(CommandManager.literal("help")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "mcscoreboard", context.getSource())))
                .then(CommandManager.literal("clear")
                        .executes(context -> clearScoreboard(context.getSource())))
                .then(CommandManager.literal("reset")
                        .executes(context -> clearScoreboard(context.getSource())))
                .then(CommandManager.literal("keep")
                        .executes(context -> keepScoreboard(context.getSource())))
                .then(CommandManager.literal("time")
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer())
                                .executes(context -> timer(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds")))))
                .then(CommandManager.literal("timer")
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer())
                                .executes(context -> timer(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds"))))));
        dispatcher.register(CommandManager.literal("mcsb").redirect(scoreboard));
    }

    private static int usage(ServerCommandSource source) {
        return SharedCommandUtil.error(source,
                "Usage: /mmoxpbar <show|hide> <skill>, /mmoxpbar reset, or /mmoxpbar disable");
    }

    private static int settingAll(ServerCommandSource source, String setting) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        if (setting.equalsIgnoreCase("reset")) {
            SharedCommandUtil.systems().uiSettings().resetXpBars(player.getUuid());
            SharedCommandUtil.systems().xpBars().hideAll(player.getUuid());
            return SharedCommandUtil.success(source, "XP bar settings have been reset.");
        }
        if (setting.equalsIgnoreCase("disable") || setting.equalsIgnoreCase("hide")) {
            for (var skill : SharedCommandUtil.api().skillRegistry().skills()) {
                if (!skill.childSkill()) SharedCommandUtil.systems().uiSettings()
                        .setXpBar(player.getUuid(), skill.id(), XpBarMode.HIDDEN);
            }
            SharedCommandUtil.systems().xpBars().hideAll(player.getUuid());
            return SharedCommandUtil.success(source, "All XP bars have been disabled.");
        }
        return usage(source);
    }

    private static int settingSkill(ServerCommandSource source, String setting, String argument) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        var skill = SharedCommandUtil.skill(argument, false);
        if (skill.isEmpty()) return SharedCommandUtil.error(source, "Unknown skill: " + argument);
        var id = skill.orElseThrow().id();
        if (setting.equalsIgnoreCase("show") || setting.equalsIgnoreCase("enable")) {
            SharedCommandUtil.systems().uiSettings().setXpBar(player.getUuid(), id, XpBarMode.ALWAYS);
            var configured = SharedCommandUtil.systems().uiConfiguration().xpBar(id);
            if (SharedCommandUtil.systems().uiConfiguration().xpBarsEnabled() && configured.enabled()) {
                SharedCommandUtil.systems().xpBars().show(player,
                        SharedCommandUtil.api().progression().query(player.getUuid(), id),
                        configured, XpBarMode.ALWAYS, source.getServer().getTicks());
            }
            return SharedCommandUtil.success(source,
                    "XP bar for " + SharedCommandUtil.cap(id.path()) + " set to SHOW.");
        }
        if (setting.equalsIgnoreCase("hide")) {
            SharedCommandUtil.systems().uiSettings().setXpBar(player.getUuid(), id, XpBarMode.HIDDEN);
            SharedCommandUtil.systems().xpBars().hide(player.getUuid(), id);
            return SharedCommandUtil.success(source,
                    "XP bar for " + SharedCommandUtil.cap(id.path()) + " set to HIDE.");
        }
        if (setting.equalsIgnoreCase("disable")) {
            // Upstream 2.3.000 accepts this per-skill enum but does not change the bar state.
            return SharedCommandUtil.success(source,
                    "XP bar for " + SharedCommandUtil.cap(id.path()) + " set to DISABLE.");
        }
        return usage(source);
    }

    private static int scoreboardHelp(ServerCommandSource source) {
        boolean enabled = SharedCommandUtil.systems().uiConfiguration().scoreboardsEnabled();
        source.sendMessage(Text.literal("FabricMMO scoreboards are currently "
                + (enabled ? "enabled." : "disabled by Scoreboard.UseScoreboards.")));
        source.sendMessage(Text.literal(
                "A scoreboard is opened by /mcstats, /mcrank, /mctop, /inspect, "
                        + "/mccooldown, or a skill command. /mcscoreboard only manages "
                        + "the board that is already displayed."));
        if (!enabled) {
            source.sendMessage(Text.literal(
                    "Set Scoreboard.UseScoreboards: true in the server's "
                            + "config/fabricmmo/config.yml, then restart the server."));
        }
        source.sendMessage(Text.literal("/mcscoreboard clear - clear the current board"));
        source.sendMessage(Text.literal("/mcscoreboard keep - keep the current board"));
        source.sendMessage(Text.literal("/mcscoreboard time <seconds> - set its remaining display time"));
        return 1;
    }

    private static int clearScoreboard(ServerCommandSource source) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        if (!scoreboardsEnabled(source)) return 0;
        if (!SharedCommandUtil.systems().scoreboards().hasBoard(player.getUuid())) {
            return SharedCommandUtil.error(source, "No FabricMMO scoreboard is currently displayed.");
        }
        SharedCommandUtil.systems().scoreboards().clear(player);
        return SharedCommandUtil.success(source, "Scoreboard cleared.");
    }

    private static int keepScoreboard(ServerCommandSource source) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        if (!scoreboardsEnabled(source)) return 0;
        if (!SharedCommandUtil.systems().uiConfiguration().allowKeep()) {
            return SharedCommandUtil.error(source, "Keeping scoreboards is disabled.");
        }
        return SharedCommandUtil.systems().scoreboards().keep(player.getUuid())
                ? SharedCommandUtil.success(source, "Scoreboard will be kept.")
                : SharedCommandUtil.error(source, "No FabricMMO scoreboard is currently displayed.");
    }

    private static int timer(ServerCommandSource source, int seconds) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        if (!scoreboardsEnabled(source)) return 0;
        int positive = Math.abs(seconds);
        return SharedCommandUtil.systems().scoreboards().setRevertSeconds(
                player.getUuid(), positive, source.getServer().getTicks())
                ? SharedCommandUtil.success(source,
                        "Scoreboard display time set to " + positive + " seconds.")
                : SharedCommandUtil.error(source, "No FabricMMO scoreboard is currently displayed.");
    }

    private static boolean scoreboardsEnabled(ServerCommandSource source) {
        if (SharedCommandUtil.systems().uiConfiguration().scoreboardsEnabled()) return true;
        SharedCommandUtil.error(source, "Scoreboards are disabled by the server.");
        return false;
    }

    private static ServerPlayerEntity player(ServerCommandSource source) {
        try { return source.getPlayerOrThrow(); }
        catch (Exception exception) {
            source.sendError(Text.literal("This command requires a player."));
            return null;
        }
    }
}
