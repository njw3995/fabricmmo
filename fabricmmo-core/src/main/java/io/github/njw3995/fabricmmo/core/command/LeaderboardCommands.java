package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.leaderboard.LeaderboardEntry;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.ui.UiSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class LeaderboardCommands {
    private LeaderboardCommands() { }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        dispatcher.register(CommandManager.literal("mctop")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mctop", true))
                .executes(context -> top(context.getSource(), "all", 1))
                .then(CommandManager.argument("skill", StringArgumentType.word())
                        .suggests(SharedCommandUtil::suggestSkills)
                        .executes(context -> top(context.getSource(),
                                StringArgumentType.getString(context, "skill"), 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> top(context.getSource(),
                                        StringArgumentType.getString(context, "skill"),
                                        IntegerArgumentType.getInteger(context, "page"))))));
        dispatcher.register(CommandManager.literal("mcrank")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mcrank", true))
                .executes(context -> rankSelf(context.getSource()))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.mcrank.others", 2))
                        .suggests(SharedCommandUtil::suggestPlayers)
                        .executes(context -> rank(context.getSource(),
                                StringArgumentType.getString(context, "player")))));
        var inspect = dispatcher.register(CommandManager.literal("inspect")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.inspect", true))
                .executes(context -> inspectUsage(context.getSource()))
                .then(CommandManager.literal("?")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "inspect", context.getSource())))
                .then(CommandManager.literal("help")
                        .executes(context -> GenericCommandHelp.show(
                                dispatcher, "inspect", context.getSource())))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(SharedCommandUtil::suggestPlayers)
                        .executes(context -> inspect(context.getSource(),
                                StringArgumentType.getString(context, "player")))));
        dispatcher.register(CommandManager.literal("mcinspect").redirect(inspect));
        dispatcher.register(CommandManager.literal("mmoinspect").redirect(inspect));
    }

    private static int top(ServerCommandSource source, String argument, int page) {
        int size = 10;
        int offset = (page - 1) * size;
        NamespacedId skillId = null;
        if (!argument.equalsIgnoreCase("all") && !argument.equalsIgnoreCase("overall")
                && !argument.equalsIgnoreCase("powerlevel")) {
            var skill = SharedCommandUtil.skill(argument, false);
            if (skill.isEmpty()) return SharedCommandUtil.error(source, "Unknown skill: " + argument);
            skillId = skill.orElseThrow().id();
        }
        List<LeaderboardEntry> rows = skillId == null
                ? SharedCommandUtil.systems().leaderboards().topPower(offset, size)
                : SharedCommandUtil.systems().leaderboards().top(skillId, offset, size);
        String title = skillId == null ? "POWER LEVEL"
                : SharedCommandUtil.cap(skillId.path()).toUpperCase(java.util.Locale.ROOT);
        ArrayList<Text> lines = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            LeaderboardEntry entry = rows.get(index);
            lines.add(Text.literal((offset + index + 1) + ". " + entry.playerName()
                    + " - " + entry.level()).formatted(Formatting.YELLOW));
        }
        display(source, UiSettings.BoardType.TOP, Text.literal(title + " Top"),
                Text.literal("---- " + title + " Top - Page " + page + " ----")
                        .formatted(Formatting.GOLD), lines);
        if (rows.isEmpty()) source.sendMessage(Text.literal("No leaderboard entries.")
                .formatted(Formatting.GRAY));
        return 1;
    }

    private static int rankSelf(ServerCommandSource source) {
        try { return rank(source, source.getPlayerOrThrow().getGameProfile().getName()); }
        catch (Exception exception) { return SharedCommandUtil.error(source, "Specify a player from console."); }
    }

    private static int rank(ServerCommandSource source, String name) {
        var id = SharedCommandUtil.playerId(source, name);
        if (id.isEmpty()) return SharedCommandUtil.error(source, "Player not found: " + name);
        UUID playerId = id.orElseThrow();
        ServerPlayerEntity viewer = source.getEntity() instanceof ServerPlayerEntity player
                ? player : null;
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerId);
        if (viewer != null && !viewer.getUuid().equals(playerId)) {
            boolean farPermission = SharedCommandUtil.systems().permissions().hasPermission(
                    source, "mcmmo.commands.mcrank.others.far", 2);
            double maxDistance = SharedCommandUtil.systems().parties().settings().inspectMaxDistance();
            boolean tooFar = target == null
                    || target.getWorld() != viewer.getWorld()
                    || target.squaredDistanceTo(viewer) > maxDistance * maxDistance;
            if (tooFar && !farPermission) {
                return SharedCommandUtil.error(source,
                        target == null ? "You cannot rank an offline player."
                                : "You are too far away to rank that player.");
            }
        }
        ArrayList<Text> lines = new ArrayList<>();
        int power = SharedCommandUtil.systems().leaderboards().rank(playerId, null);
        lines.add(Text.literal("Power Level: " + (power == 0 ? "unranked" : "#" + power))
                .formatted(Formatting.YELLOW));
        for (var skill : SharedCommandUtil.api().skillRegistry().skills()) {
            if (skill.childSkill()) continue;
            int position = SharedCommandUtil.systems().leaderboards().rank(playerId, skill.id());
            if (position > 0) lines.add(Text.literal(
                    SharedCommandUtil.cap(skill.id().path()) + ": #" + position));
        }
        display(source, UiSettings.BoardType.RANK,
                Text.literal("Ranks: " + SharedCommandUtil.playerName(playerId)),
                Text.literal("---- Ranks for " + SharedCommandUtil.playerName(playerId) + " ----")
                        .formatted(Formatting.GOLD), lines);
        return 1;
    }


    private static int inspectUsage(ServerCommandSource source) {
        source.sendMessage(Text.literal("/inspect <player> - View a nearby player's mcMMO skill levels. "
                + "Inspecting distant or offline players requires mcmmo.commands.inspect.far.")
                .formatted(Formatting.YELLOW));
        return 1;
    }

    private static int inspect(ServerCommandSource source, String name) {
        var id = SharedCommandUtil.playerId(source, name);
        if (id.isEmpty()) return SharedCommandUtil.error(source, "Player not found: " + name);
        UUID targetId = id.orElseThrow();
        ServerPlayerEntity viewer = source.getEntity() instanceof ServerPlayerEntity player ? player : null;
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetId);
        boolean farPermission = SharedCommandUtil.systems().permissions().hasPermission(
                source, "mcmmo.commands.inspect.far", 2);
        if (target == null && viewer != null && !farPermission) {
            return SharedCommandUtil.error(source, "You do not have permission to inspect offline players.");
        }
        if (target != null && viewer != null) {
            boolean hidden = !SharedCommandUtil.systems().visibility().visibleTo(target, viewer);
            if (hidden && !SharedCommandUtil.systems().permissions().hasPermission(
                    source, "mcmmo.commands.inspect.hidden", 2)) {
                return SharedCommandUtil.error(source, "Player not found: " + name);
            }
            double maxDistance = SharedCommandUtil.systems().parties().settings().inspectMaxDistance();
            boolean tooFar = target.getWorld() != viewer.getWorld()
                    || target.squaredDistanceTo(viewer) > maxDistance * maxDistance;
            if (tooFar && !farPermission) return SharedCommandUtil.error(source,
                    "You are too far away to inspect that player.");
        }

        List<Text> lines = new ArrayList<>();
        int power = 0;
        for (var entry : SharedCommandUtil.api().progression().queryAll(targetId).entrySet()) {
            if (SharedCommandUtil.api().skillRegistry().find(entry.getKey())
                    .map(definition -> definition.childSkill()).orElse(false)) continue;
            power += entry.getValue().level();
            lines.add(Text.literal(SharedCommandUtil.cap(entry.getKey().path()) + ": "
                    + entry.getValue().level()));
        }
        Text header = Text.literal("---- " + SharedCommandUtil.playerName(targetId)
                + " - Power " + power + " ----").formatted(Formatting.GOLD);
        display(source, UiSettings.BoardType.INSPECT,
                Text.literal("mcMMO Stats: " + SharedCommandUtil.playerName(targetId)), header, lines);
        return 1;
    }

    private static void display(ServerCommandSource source, UiSettings.BoardType type,
            Text boardTitle, Text chatHeader, List<Text> lines) {
        CommandUiDisplay.configured(source, type, boardTitle, chatHeader, lines);
    }
}
