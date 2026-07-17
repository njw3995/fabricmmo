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
import io.github.njw3995.fabricmmo.core.skill.mining.MiningCommandFormatter;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningCommandSnapshot;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningDropSettings;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningProbability;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningPerks;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningSettings;
import java.io.IOException;
import java.io.UncheckedIOException;
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
    }

    private static void registerMcmmo(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        Predicate<ServerCommandSource> descriptionPermission = source -> permissions.hasPermission(
                source, PermissionNodes.MCMMO_DESCRIPTION, true);
        Predicate<ServerCommandSource> helpPermission = source -> permissions.hasPermission(
                source, PermissionNodes.MCMMO_HELP, true);

        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("mcmmo")
                .requires(descriptionPermission)
                .executes(context -> showDescription(context.getSource()));
        root.then(CommandManager.literal("help").requires(helpPermission)
                .executes(context -> showHelp(context.getSource())));
        root.then(CommandManager.literal("commands").requires(helpPermission)
                .executes(context -> showHelp(context.getSource())));
        root.then(CommandManager.literal("?").requires(helpPermission)
                .executes(context -> showHelp(context.getSource())));
        dispatcher.register(root);
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
                .then(CommandManager.literal("?")
                        .executes(context -> showMiningGuide(context, 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> showMiningGuide(
                                        context,
                                        IntegerArgumentType.getInteger(context, "page")))))
                .then(CommandManager.literal("help")
                        .executes(context -> showMiningGuide(context, 1))
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> showMiningGuide(
                                        context,
                                        IntegerArgumentType.getInteger(context, "page"))))));
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

    private static int showHelp(ServerCommandSource source) {
        source.sendMessage(Text.literal("---- mcMMO Commands ----"));
        source.sendMessage(Text.literal("/mcstats - Show your skill levels and XP"));
        source.sendMessage(Text.literal("/mining - Show Mining stats and ability details"));
        source.sendMessage(Text.literal("/addxp [player] <skill|all> <amount> - Award skill XP"));
        source.sendMessage(Text.literal("Additional upstream commands are not implemented yet."));
        return Command.SINGLE_SUCCESS;
    }

    private static int showStats(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        var lines = StatsTextFormatter.format(
                api.skillRegistry(), api.progression().queryAll(player.getUuid()));
        lines.forEach(context.getSource()::sendMessage);
        return Command.SINGLE_SUCCESS;
    }


    private static int showMining(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        var progress = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.MINING);
        MiningSettings settings = FabricMmoFabricRuntime.miningSettings();
        MiningDropSettings drops = FabricMmoFabricRuntime.miningDropSettings();
        var permissionService = new io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService();
        boolean lucky = permissionService.hasPermission(
                player.getCommandSource(), PermissionNodes.MINING_LUCKY, false);
        int level = progress.level();
        int rank = settings.blastRank(level);
        boolean showDoubleDrops = drops.doubleDropsUnlocked(level, settings.progressionMode())
                && permissionService.hasPermission(
                        player.getCommandSource(), PermissionNodes.MINING_DOUBLE_DROPS, true);
        boolean showMotherLode = drops.motherLodeUnlocked(level, settings.progressionMode())
                && permissionService.hasPermission(
                        player.getCommandSource(), PermissionNodes.MINING_MOTHER_LODE, true);
        boolean showSuperBreaker = level >= settings.superBreakerUnlockLevel()
                && permissionService.hasPermission(
                        player.getCommandSource(), PermissionNodes.MINING_SUPER_BREAKER, true);
        boolean showBlastMining = rank > 0
                && permissionService.hasPermission(
                        player.getCommandSource(), PermissionNodes.MINING_BLAST_MINING, true);
        boolean showBiggerBombs = level >= settings.biggerBombsUnlockLevel()
                && permissionService.hasPermission(
                        player.getCommandSource(), PermissionNodes.MINING_BIGGER_BOMBS, true);
        boolean showDemolitionsExpertise = level >= settings.demolitionsExpertiseUnlockLevel()
                && permissionService.hasPermission(
                        player.getCommandSource(), PermissionNodes.MINING_DEMOLITIONS_EXPERTISE, true);
        int superBreakerCooldown = MiningPerks.cooldownSeconds(
                settings.superBreakerCooldownSeconds(), player.getCommandSource(), permissionService);
        int blastCooldown = MiningPerks.cooldownSeconds(
                settings.blastMiningCooldownSeconds(), player.getCommandSource(), permissionService);
        int activationBonus = MiningPerks.activationBonusSeconds(
                player.getCommandSource(), permissionService);
        try {
            MiningCommandSnapshot snapshot = new MiningCommandSnapshot(
                    level,
                    progress.xp(),
                    progress.xpToNextLevel(),
                    drops.doubleDropsUnlocked(level, settings.progressionMode())
                            ? MiningProbability.chance(
                                    level,
                                    drops.doubleDropsMaxLevel(settings.progressionMode()),
                                    drops.doubleDropsChanceMaxPercent(),
                                    lucky) * 100.0D
                            : 0.0D,
                    drops.motherLodeUnlocked(level, settings.progressionMode())
                            ? MiningProbability.chance(
                                    level,
                                    drops.motherLodeMaxLevel(settings.progressionMode()),
                                    drops.motherLodeChanceMaxPercent(),
                                    lucky) * 100.0D
                            : 0.0D,
                    settings.superBreakerDurationSeconds(level) + activationBonus,
                    FabricMmoFabricRuntime.miningAbilities()
                            .superBreakerCooldownRemaining(player.getUuid(), superBreakerCooldown),
                    FabricMmoFabricRuntime.miningAbilities()
                            .isSuperBreakerActive(player.getUuid()),
                    FabricMmoFabricRuntime.miningAbilities()
                            .superBreakerSecondsRemaining(player.getUuid()),
                    rank,
                    MiningSettings.BLAST_RANKS,
                    settings.oreBonusFraction(rank) * 100.0D,
                    settings.dropMultiplier(rank),
                    settings.blastRadiusModifier(rank),
                    settings.blastDamageDecreasePercent(rank),
                    FabricMmoFabricRuntime.miningAbilities()
                            .blastCooldownRemaining(player.getUuid(), blastCooldown),
                    showDoubleDrops,
                    showMotherLode,
                    showSuperBreaker,
                    showBlastMining,
                    showBiggerBombs,
                    showDemolitionsExpertise);
            context.getSource().sendMessage(
                    io.github.njw3995.fabricmmo.core.skill.mining.MiningMessages.header("MINING"));
            MiningCommandFormatter.format(snapshot).forEach(line ->
                    context.getSource().sendMessage(styledMiningLine(line)));
            return Command.SINGLE_SUCCESS;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Mining ability state", exception);
        }
    }

    private static int showMiningGuide(
            CommandContext<ServerCommandSource> context,
            int page) {
        java.util.List<String> guide = MiningCommandFormatter.guide();
        int pageSize = 8;
        int totalPages = Math.max(1, (guide.size() + pageSize - 1) / pageSize);
        if (page > totalPages) {
            context.getSource().sendError(Text.literal(
                    "That page does not exist. There are " + totalPages + " pages."));
            return 0;
        }
        context.getSource().sendMessage(
                io.github.njw3995.fabricmmo.core.skill.mining.MiningMessages.header("MINING GUIDE"));
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, guide.size());
        guide.subList(start, end).forEach(line ->
                context.getSource().sendMessage(Text.literal(line)
                        .formatted(net.minecraft.util.Formatting.GRAY)));
        context.getSource().sendMessage(Text.literal("Page " + page + " of " + totalPages)
                .formatted(net.minecraft.util.Formatting.GOLD));
        return Command.SINGLE_SUCCESS;
    }


    private static Text styledMiningLine(String line) {
        int separator = line.indexOf(':');
        if (separator < 0) {
            return Text.literal(line).formatted(net.minecraft.util.Formatting.GREEN);
        }
        return Text.literal(line.substring(0, separator + 1) + " ")
                .formatted(net.minecraft.util.Formatting.DARK_AQUA)
                .append(Text.literal(line.substring(separator + 1).trim())
                        .formatted(net.minecraft.util.Formatting.GREEN));
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
