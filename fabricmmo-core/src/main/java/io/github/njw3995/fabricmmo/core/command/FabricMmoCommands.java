package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.command.CommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class FabricMmoCommands {
    private static final int ADMIN_FALLBACK_LEVEL = 2;
    private static final String ALL_SKILLS = "all";
    private static final String ADD_XP_USAGE =
            "Proper usage is /addxp [player] <skill|all> <xp>";

    private FabricMmoCommands() {
    }

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment,
            CommandPermissionService permissions) {
        registerMcmmo(dispatcher, permissions);
        registerMcstats(dispatcher, permissions);
        registerMining(dispatcher, permissions);
        registerAddXp(dispatcher, permissions);
        PlayerUtilityCommands.register(dispatcher, permissions);
        ProgressionAdminCommands.register(dispatcher, permissions);
        SharedCommandRegistrar.register(dispatcher, permissions);
        GenericCommandHelp.attach(dispatcher);
    }

    private static void registerMcmmo(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        Predicate<ServerCommandSource> descriptionPermission = source -> permissions.hasPermission(
                source, PermissionNodes.MCMMO_DESCRIPTION, true);
        Predicate<ServerCommandSource> helpPermission = source -> permissions.hasPermission(
                source, PermissionNodes.MCMMO_HELP, true);

        dispatcher.register(rootCommand(
                "mcmmo", descriptionPermission, helpPermission, permissions));

        Predicate<ServerCommandSource> fabricRootPermission = source -> permissions.hasPermission(
                source, PermissionNodes.FABRICMMO_ROOT, true);
        dispatcher.register(rootCommand(
                "fabricmmo", fabricRootPermission, helpPermission, permissions));
        dispatcher.register(rootCommand(
                "fmmo", fabricRootPermission, helpPermission, permissions));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> rootCommand(
            String literal,
            Predicate<ServerCommandSource> descriptionPermission,
            Predicate<ServerCommandSource> helpPermission,
            CommandPermissionService permissions) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(literal)
                .requires(descriptionPermission)
                .executes(context -> showDescription(context.getSource()));
        root.then(CommandManager.literal("help").requires(helpPermission)
                .executes(context -> InformationCommands.showHelpPage(context.getSource(), 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> InformationCommands.showHelpPage(context.getSource(), IntegerArgumentType.getInteger(context, "page")))));
        root.then(CommandManager.literal("commands").requires(helpPermission)
                .executes(context -> InformationCommands.showHelpPage(context.getSource(), 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> InformationCommands.showHelpPage(context.getSource(), IntegerArgumentType.getInteger(context, "page")))));
        root.then(CommandManager.literal("?").requires(helpPermission)
                .executes(context -> InformationCommands.showHelpPage(context.getSource(), 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> InformationCommands.showHelpPage(context.getSource(), IntegerArgumentType.getInteger(context, "page")))));
        return root;
    }

    private static void registerMcstats(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        Predicate<ServerCommandSource> statsPermission = source -> permissions.hasPermission(
                source, PermissionNodes.MCSTATS, true);
        var node = dispatcher.register(CommandManager.literal("mcstats")
                .requires(statsPermission)
                .executes(FabricMmoCommands::showStats));
        dispatcher.register(CommandManager.literal("stats")
                .requires(statsPermission)
                .redirect(node));
    }


    private static void registerMining(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        Predicate<ServerCommandSource> miningPermission = source -> permissions.hasPermission(
                source, PermissionNodes.MINING_COMMAND, true);
        dispatcher.register(CommandManager.literal("mining")
                .requires(miningPermission)
                .executes(FabricMmoCommands::showMining)
                .then(CommandManager.literal("keep")
                        .executes(FabricMmoCommands::keepMiningBoard))
                .then(CommandManager.literal("?")
                        .executes(context -> InformationCommands.showSkillGuide(context.getSource(), "mining", 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> InformationCommands.showSkillGuide(
                                        context.getSource(),
                                        "mining",
                                        IntegerArgumentType.getInteger(context, "page")))))
                .then(CommandManager.literal("help")
                        .executes(context -> InformationCommands.showSkillGuide(context.getSource(), "mining", 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> InformationCommands.showSkillGuide(
                                        context.getSource(),
                                        "mining",
                                        IntegerArgumentType.getInteger(context, "page"))))));
    }

    private static int keepMiningBoard(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        var systems = SharedCommandUtil.systems();
        var settings = systems.uiConfiguration();
        if (!settings.scoreboardsEnabled() || !settings.allowKeep()
                || !settings.board(io.github.njw3995.fabricmmo.core.ui.UiSettings.BoardType.SKILL).enabled()) {
            return SharedCommandUtil.error(source,
                    LegacyText.strip(systems.locale().text("Commands.Disabled")));
        }
        return InformationCommands.keepSkillBoard(source, "mining");
    }

    private static void registerAddXp(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        Predicate<ServerCommandSource> selfPermission = source -> permissions.hasPermission(
                source, PermissionNodes.ADD_XP, ADMIN_FALLBACK_LEVEL);
        Predicate<ServerCommandSource> othersPermission = source -> permissions.hasPermission(
                source, PermissionNodes.ADD_XP_OTHERS, ADMIN_FALLBACK_LEVEL);

        dispatcher.register(CommandManager.literal("addxp")
                .requires(source -> selfPermission.test(source) || othersPermission.test(source))
                .executes(context -> showAddXpUsage(context.getSource()))
                .then(CommandManager.argument("targetOrSkill", StringArgumentType.word())
                        .suggests((context, builder) -> suggestAddXpFirstArgument(
                                context, builder, selfPermission, othersPermission))
                        .executes(context -> showAddXpUsage(context.getSource()))
                        .then(CommandManager.argument("skillOrAmount", StringArgumentType.word())
                                .suggests(FabricMmoCommands::suggestAddXpSecondArgument)
                                .executes(context -> addXpSelf(context, selfPermission))
                                .then(CommandManager.argument(
                                                "amount", StringArgumentType.word())
                                        .requires(othersPermission)
                                        .executes(FabricMmoCommands::addXpOther)))));
    }

    private static int showDescription(ServerCommandSource source) {
        source.sendMessage(Text.literal("mcMMO adds skill progression to core Minecraft gameplay."));
        source.sendMessage(Text.literal("FabricMMO is a Fabric-native port tracking mcMMO 2.3.000."));
        source.sendMessage(Text.literal("Version 0.1.0-SNAPSHOT"));
        return Command.SINGLE_SUCCESS;
    }

    private static int showHelp(
            ServerCommandSource source,
            CommandPermissionService permissions) {
        source.sendMessage(Text.literal("---- mcMMO Commands ----"));
        sendHelpIfAllowed(source, permissions, PermissionNodes.MCSTATS, true,
                "/mcstats - Show your skill levels and XP");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MMOPOWER, true,
                "/mmopower - Show your power level");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MINING_COMMAND, true,
                "/mining - Show Mining stats and ability details");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MCCOOLDOWN, true,
                "/mccooldown - Show ability cooldowns");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MCABILITY, true,
                "/mcability - Toggle ability activation");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MCNOTIFY, true,
                "/mcnotify - Toggle skill notifications");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MCLEVELUPSOUND, true,
                "/mclevelupsound - Toggle level-up sounds");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MCREFRESH,
                ADMIN_FALLBACK_LEVEL,
                "/mcrefresh [player] - Reset ability cooldowns");
        sendHelpIfAllowed(source, permissions, PermissionNodes.ADD_XP,
                ADMIN_FALLBACK_LEVEL,
                "/addxp [player] <skill|all> <amount> - Award skill XP");
        sendHelpIfAllowed(source, permissions, PermissionNodes.ADD_LEVELS,
                ADMIN_FALLBACK_LEVEL,
                "/addlevels [player] <skill|all> <amount> - Award skill levels");
        sendHelpIfAllowed(source, permissions, PermissionNodes.MMO_EDIT,
                ADMIN_FALLBACK_LEVEL,
                "/mmoedit [player] <skill|all> <level> - Set a skill level");
        sendHelpIfAllowed(source, permissions, PermissionNodes.SKILL_RESET,
                ADMIN_FALLBACK_LEVEL,
                "/skillreset [player] <skill|all> - Reset skill progress");
        return Command.SINGLE_SUCCESS;
    }

    private static void sendHelpIfAllowed(
            ServerCommandSource source,
            CommandPermissionService permissions,
            String permission,
            boolean fallback,
            String line) {
        if (permissions.hasPermission(source, permission, fallback)) {
            source.sendMessage(Text.literal(line));
        }
    }

    private static void sendHelpIfAllowed(
            ServerCommandSource source,
            CommandPermissionService permissions,
            String permission,
            int fallbackLevel,
            String line) {
        if (permissions.hasPermission(source, permission, fallbackLevel)) {
            source.sendMessage(Text.literal(line));
        }
    }

    private static int showStats(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        var lines = StatsTextFormatter.format(
                api.skillRegistry(), api.progression().queryAll(player.getUuid()));
        List<Text> boardLines = lines.size() > 2
                ? List.copyOf(lines.subList(2, lines.size())) : lines;
        CommandUiDisplay.configured(
                context.getSource(),
                io.github.njw3995.fabricmmo.core.ui.UiSettings.BoardType.STATS,
                Text.literal("mcMMO Stats"),
                lines,
                boardLines);
        return Command.SINGLE_SUCCESS;
    }


    private static int showMining(CommandContext<ServerCommandSource> context) {
        return InformationCommands.showSkill(context.getSource(), "mining");
    }

    private static int addXpSelf(
            CommandContext<ServerCommandSource> context,
            Predicate<ServerCommandSource> selfPermission) throws CommandSyntaxException {
        if (!selfPermission.test(context.getSource())) {
            context.getSource().sendError(Text.literal("You do not have permission to use /addxp."));
            return 0;
        }
        String skillArgument = StringArgumentType.getString(context, "targetOrSkill");
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        if (!skillArgument.equalsIgnoreCase(ALL_SKILLS)
                && SkillArgumentResolver.resolve(api.skillRegistry(), skillArgument, false)
                        .isEmpty()) {
            return showAddXpUsage(context.getSource());
        }
        Integer amount = parsePositiveXp(
                context.getSource(), StringArgumentType.getString(context, "skillOrAmount"));
        if (amount == null) {
            return 0;
        }
        return awardXp(
                context.getSource(),
                context.getSource().getPlayerOrThrow(),
                skillArgument,
                amount);
    }

    private static int addXpOther(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "targetOrSkill");
        ServerPlayerEntity target = context.getSource().getServer()
                .getPlayerManager()
                .getPlayer(playerName);
        if (target == null) {
            context.getSource().sendError(Text.literal("Player not found: " + playerName));
            return 0;
        }
        Integer amount = parsePositiveXp(
                context.getSource(), StringArgumentType.getString(context, "amount"));
        if (amount == null) {
            return 0;
        }
        return awardXp(
                context.getSource(),
                target,
                StringArgumentType.getString(context, "skillOrAmount"),
                amount);
    }

    private static Integer parsePositiveXp(ServerCommandSource source, String value) {
        try {
            int amount = Integer.parseInt(value);
            if (amount > 0) {
                return amount;
            }
        } catch (NumberFormatException ignored) {
            // Report the same validation message for malformed and non-positive values.
        }
        source.sendError(Text.literal("XP must be a positive whole number."));
        return null;
    }

    private static int showAddXpUsage(ServerCommandSource source) {
        source.sendError(Text.literal(ADD_XP_USAGE));
        return 0;
    }

    private static int awardXp(
            ServerCommandSource source,
            ServerPlayerEntity target,
            String skillArgument,
            int amount) {
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        if (skillArgument.equalsIgnoreCase(ALL_SKILLS)) {
            int appliedSkills = 0;
            for (SkillDefinition skill : api.skillRegistry().skills()) {
                if (skill.childSkill()) {
                    continue;
                }
                XpAwardResult result = award(api, target, skill, amount);
                if (result.status() == XpAwardResult.Status.APPLIED) {
                    appliedSkills++;
                }
            }
            int awardedSkillCount = appliedSkills;
            source.sendFeedback(() -> Text.literal(
                    "Awarded " + amount + " XP in " + awardedSkillCount + " skills to "
                            + target.getName().getString() + '.'), false);
            return awardedSkillCount;
        }

        var skill = SkillArgumentResolver.resolve(api.skillRegistry(), skillArgument, false);
        if (skill.isEmpty()) {
            source.sendError(Text.literal("Unknown or ambiguous skill: " + skillArgument));
            return 0;
        }
        XpAwardResult result = award(api, target, skill.orElseThrow(), amount);
        if (result.status() != XpAwardResult.Status.APPLIED) {
            source.sendError(Text.literal("XP award rejected: " + result.detail()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal(
                "Awarded " + result.appliedXp() + " XP in "
                        + SkillArgumentResolver.displayName(skill.orElseThrow().id())
                        + " to " + target.getName().getString() + '.'), false);
        return Command.SINGLE_SUCCESS;
    }

    private static XpAwardResult award(
            FabricMmoApi api,
            ServerPlayerEntity target,
            SkillDefinition skill,
            int amount) {
        return api.progression().award(new XpAwardRequest(
                target.getUuid(),
                skill.id(),
                CoreXpSources.commandSourceId(skill.id()),
                amount,
                Map.of("reason", "command")));
    }

    private static CompletableFuture<Suggestions> suggestAddXpFirstArgument(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder,
            Predicate<ServerCommandSource> selfPermission,
            Predicate<ServerCommandSource> othersPermission) {
        Set<String> suggestions = new LinkedHashSet<>();
        if (othersPermission.test(context.getSource())) {
            suggestions.addAll(context.getSource().getPlayerNames());
        }
        if (selfPermission.test(context.getSource())) {
            suggestions.addAll(skillSuggestions());
        }
        return CommandSource.suggestMatching(suggestions, builder);
    }

    private static CompletableFuture<Suggestions> suggestAddXpSecondArgument(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder) {
        String firstArgument = StringArgumentType.getString(context, "targetOrSkill");
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        if (firstArgument.equalsIgnoreCase(ALL_SKILLS)
                || SkillArgumentResolver.resolve(api.skillRegistry(), firstArgument, false)
                        .isPresent()) {
            return builder.buildFuture();
        }
        return CommandSource.suggestMatching(skillSuggestions(), builder);
    }

    private static List<String> skillSuggestions() {
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        return SkillArgumentResolver.suggestions(api.skillRegistry(), true);
    }
}
