package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.leaderboard.LeaderboardEntry;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.ui.ScoreboardValueLine;
import io.github.njw3995.fabricmmo.core.ui.UiSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class LeaderboardCommands {
    private static final List<String> UPSTREAM_NON_CHILD_SKILL_ORDER = List.of(
            "acrobatics", "alchemy", "archery", "axes", "crossbows",
            "excavation", "fishing", "herbalism", "maces", "mining",
            "repair", "swords", "taming", "tridents", "unarmed",
            "woodcutting");

    private LeaderboardCommands() { }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        dispatcher.register(CommandManager.literal("mctop")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mctop", true))
                .executes(context -> top(context.getSource(), null, 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(Integer.MIN_VALUE))
                        .executes(context -> top(context.getSource(), null,
                                normalizePage(IntegerArgumentType.getInteger(context, "page")))))
                .then(CommandManager.argument("skill", StringArgumentType.word())
                        .suggests(SharedCommandUtil::suggestSkills)
                        .executes(context -> topSkill(context.getSource(),
                                StringArgumentType.getString(context, "skill"), 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(Integer.MIN_VALUE))
                                .executes(context -> topSkill(context.getSource(),
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

    private static int topSkill(ServerCommandSource source, String argument, int page) {
        var skill = SharedCommandUtil.skill(argument, true);
        if (skill.isEmpty()) {
            return SharedCommandUtil.error(source, "Unknown skill: " + argument);
        }
        SkillDefinition definition = skill.orElseThrow();
        if (definition.childSkill()) {
            return SharedCommandUtil.error(source,
                    "Child skills are not supported by this command.");
        }
        if (!canViewTop(source, definition)) {
            return SharedCommandUtil.error(source,
                    "You do not have permission to view that leaderboard.");
        }
        return top(source, definition.id(), normalizePage(page));
    }

    private static int normalizePage(int page) {
        return Math.abs(page);
    }

    private static boolean canViewTop(ServerCommandSource source, SkillDefinition skill) {
        return SharedCommandUtil.systems().permissions().hasPermission(
                source, "mcmmo.commands.mctop." + skill.id().path().toLowerCase(Locale.ROOT), true);
    }

    private static boolean canViewRankBoardSkill(
            ServerCommandSource source, SkillDefinition skill) {
        return SharedCommandUtil.systems().permissions().hasPermission(
                source, "mcmmo.skills." + skill.id().path().toLowerCase(Locale.ROOT), true);
    }

    private static int top(ServerCommandSource source, NamespacedId skillId, int page) {
        int size = 10;
        int offset = (page - 1) * size;
        List<LeaderboardEntry> rows = skillId == null
                ? SharedCommandUtil.systems().leaderboards().topPower(offset, size)
                : SharedCommandUtil.systems().leaderboards().top(skillId, offset, size);

        ArrayList<Text> chatLines = new ArrayList<>();
        chatLines.add(topChatHeader(skillId));
        ArrayList<ScoreboardValueLine> boardLines = new ArrayList<>();
        String viewerName = source.getEntity() instanceof ServerPlayerEntity player
                ? player.getGameProfile().getName()
                : null;
        for (int index = 0; index < rows.size(); index++) {
            LeaderboardEntry entry = rows.get(index);
            int place = offset + index + 1;
            chatLines.add(Text.empty()
                    .append(Text.literal(String.format(Locale.ROOT, "%2d. ", place)))
                    .append(Text.literal(entry.playerName()).formatted(Formatting.GREEN))
                    .append(Text.literal(" - ").formatted(Formatting.WHITE))
                    .append(Text.literal(Integer.toString(entry.level()))
                            .formatted(Formatting.WHITE)));
            Text boardName = viewerName != null && viewerName.equals(entry.playerName())
                    ? Text.literal("--You--").formatted(Formatting.GOLD)
                    : Text.literal(entry.playerName());
            boardLines.add(new ScoreboardValueLine(boardName, entry.level()));
        }
        CommandUiDisplay.configuredValues(
                source, UiSettings.BoardType.TOP, topBoardTitle(skillId, page),
                chatLines, boardLines);
        if (source.getEntity() instanceof ServerPlayerEntity) {
            source.sendMessage(Text.empty()
                    .append(Text.literal("Tip: Use ").formatted(Formatting.GOLD))
                    .append(Text.literal("/mcrank").formatted(Formatting.RED))
                    .append(Text.literal(" to view all of your personal rankings!")
                            .formatted(Formatting.GOLD)));
        }
        return 1;
    }

    private static Text topChatHeader(NamespacedId skillId) {
        if (skillId == null) {
            return Text.empty()
                    .append(Text.literal("--mcMMO").formatted(Formatting.YELLOW))
                    .append(Text.literal(" Power Level ").formatted(Formatting.BLUE))
                    .append(Text.literal("Leaderboard--").formatted(Formatting.YELLOW));
        }
        return Text.empty()
                .append(Text.literal("-"))
                .append(Text.literal("-mcMMO ").formatted(Formatting.YELLOW))
                .append(Text.literal(SharedCommandUtil.cap(skillId.path()))
                        .formatted(Formatting.BLUE))
                .append(Text.literal(" Leaderboard--").formatted(Formatting.YELLOW));
    }

    private static Text topBoardTitle(NamespacedId skillId, int page) {
        int end = page * 10;
        int start = end - 9;
        String range = String.format(Locale.ROOT, " (%2d - %2d)", start, end);
        if (skillId == null) {
            return Text.literal("Power Level" + range).formatted(Formatting.GOLD);
        }
        return Text.literal(SharedCommandUtil.cap(skillId.path()).toUpperCase(Locale.ROOT) + range)
                .formatted(Formatting.GREEN);
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
        String playerName = SharedCommandUtil.playerName(playerId);
        ArrayList<Text> chatLines = new ArrayList<>();
        chatLines.add(Text.literal("-=PERSONAL RANKINGS=-").formatted(Formatting.GOLD));
        chatLines.add(Text.empty()
                .append(Text.literal("Rankings for ").formatted(Formatting.YELLOW))
                .append(Text.literal(playerName).formatted(Formatting.WHITE)));

        ArrayList<ScoreboardValueLine> boardLines = new ArrayList<>();
        for (SkillDefinition skill : orderedRankSkills()) {
            int position = SharedCommandUtil.systems().leaderboards().rank(playerId, skill.id());
            String displayName = SharedCommandUtil.cap(skill.id().path());
            chatLines.add(rankChatLine(displayName, position));
            if (position > 0 && canViewRankBoardSkill(source, skill)) {
                boardLines.add(new ScoreboardValueLine(
                        Text.literal(displayName.toUpperCase(Locale.ROOT))
                                .formatted(Formatting.GREEN),
                        position));
            }
        }

        int power = SharedCommandUtil.systems().leaderboards().rank(playerId, null);
        chatLines.add(overallRankChatLine(power));
        if (power > 0) {
            boardLines.add(new ScoreboardValueLine(
                    Text.literal("Power Level").formatted(Formatting.GOLD), power));
        }

        CommandUiDisplay.configuredValues(
                source, UiSettings.BoardType.RANK,
                Text.literal("mcMMO Rankings").formatted(Formatting.YELLOW),
                chatLines, boardLines);
        return 1;
    }


    private static List<SkillDefinition> orderedRankSkills() {
        ArrayList<SkillDefinition> ordered = new ArrayList<>();
        Set<NamespacedId> seen = new HashSet<>();
        for (String path : UPSTREAM_NON_CHILD_SKILL_ORDER) {
            SharedCommandUtil.api().skillRegistry().find(SharedCommandUtil.coreSkill(path))
                    .filter(skill -> !skill.childSkill())
                    .ifPresent(skill -> {
                        ordered.add(skill);
                        seen.add(skill.id());
                    });
        }
        SharedCommandUtil.api().skillRegistry().skills().stream()
                .filter(skill -> !skill.childSkill())
                .filter(skill -> !seen.contains(skill.id()))
                .sorted(Comparator.comparing(skill -> skill.id().toString()))
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private static Text rankChatLine(String skillName, int position) {
        Text result = Text.empty()
                .append(Text.literal(skillName).formatted(Formatting.YELLOW))
                .append(Text.literal(" - ").formatted(Formatting.GREEN))
                .append(Text.literal("Rank ").formatted(Formatting.GOLD))
                .append(Text.literal("#").formatted(Formatting.WHITE));
        return result.copy().append(position > 0
                ? Text.literal(Integer.toString(position)).formatted(Formatting.GREEN)
                : Text.literal("Unranked").formatted(Formatting.WHITE));
    }

    private static Text overallRankChatLine(int position) {
        Text result = Text.empty()
                .append(Text.literal("Overall"))
                .append(Text.literal(" - ").formatted(Formatting.GREEN))
                .append(Text.literal("Rank ").formatted(Formatting.GOLD))
                .append(Text.literal("#").formatted(Formatting.WHITE));
        return result.copy().append(position > 0
                ? Text.literal(Integer.toString(position)).formatted(Formatting.GREEN)
                : Text.literal("Unranked").formatted(Formatting.WHITE));
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
