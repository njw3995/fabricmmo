package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ProgressionAdminCommands {
    private static final String ALL = "all";
    private static final String SILENT = "-s";

    private ProgressionAdminCommands() {
    }

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        registerLevel(dispatcher, permissions, Operation.ADD, "levels");
        registerLevel(dispatcher, permissions, Operation.SET, "level");
        registerReset(dispatcher, permissions);
    }

    private static void registerLevel(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions,
            Operation operation,
            String valueArgument) {
        dispatcher.register(CommandManager.literal(operation.literal)
                .requires(source -> permissions.hasPermission(source, operation.self, 2)
                        || permissions.hasPermission(source, operation.others, 2))
                .executes(context -> GenericCommandHelp.show(
                        dispatcher, operation.literal, context.getSource()))
                .then(CommandManager.argument("targetOrSkill", StringArgumentType.word())
                        .suggests((context, builder) -> suggestFirstArgument(
                                context.getSource(), builder, permissions, operation))
                        .then(CommandManager.argument("skillOrValue", StringArgumentType.word())
                                .suggests((context, builder) -> suggestSecondArgument(
                                        context.getSource(),
                                        builder,
                                        StringArgumentType.getString(context, "targetOrSkill"),
                                        permissions,
                                        operation))
                                .executes(context -> applySelfFromContext(
                                        context.getSource(),
                                        operation,
                                        StringArgumentType.getString(context, "targetOrSkill"),
                                        StringArgumentType.getString(context, "skillOrValue"),
                                        false))
                                .then(CommandManager.literal(SILENT)
                                        .executes(context -> applySelfFromContext(
                                                context.getSource(),
                                                operation,
                                                StringArgumentType.getString(context, "targetOrSkill"),
                                                StringArgumentType.getString(context, "skillOrValue"),
                                                true)))
                                .then(CommandManager.argument(valueArgument, IntegerArgumentType.integer())
                                        .requires(source -> permissions.hasPermission(
                                                source, operation.others, 2))
                                        .executes(context -> applyOther(
                                                context.getSource(),
                                                operation,
                                                StringArgumentType.getString(context, "targetOrSkill"),
                                                StringArgumentType.getString(context, "skillOrValue"),
                                                IntegerArgumentType.getInteger(context, valueArgument),
                                                false))
                                        .then(CommandManager.literal(SILENT)
                                                .executes(context -> applyOther(
                                                        context.getSource(),
                                                        operation,
                                                        StringArgumentType.getString(context, "targetOrSkill"),
                                                        StringArgumentType.getString(context, "skillOrValue"),
                                                        IntegerArgumentType.getInteger(context, valueArgument),
                                                        true)))))));
    }

    private static CompletableFuture<Suggestions> suggestFirstArgument(
            ServerCommandSource source,
            SuggestionsBuilder builder,
            CommandPermissionService permissions,
            Operation operation) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        if (permissions.hasPermission(source, operation.self, 2)) {
            suggestions.addAll(SharedCommandUtil.skills(true));
        }
        if (permissions.hasPermission(source, operation.others, 2)) {
            suggestions.addAll(source.getPlayerNames());
            suggestions.addAll(SharedCommandUtil.systems().identities().identities().values());
        }
        return CommandSource.suggestMatching(suggestions, builder);
    }

    private static CompletableFuture<Suggestions> suggestSecondArgument(
            ServerCommandSource source,
            SuggestionsBuilder builder,
            String firstArgument,
            CommandPermissionService permissions,
            Operation operation) {
        if (permissions.hasPermission(source, operation.others, 2)
                && SharedCommandUtil.playerId(source, firstArgument).isPresent()) {
            return CommandSource.suggestMatching(SharedCommandUtil.skills(true), builder);
        }
        return builder.buildFuture();
    }

    private static int applySelfFromContext(
            ServerCommandSource source,
            Operation operation,
            String skill,
            String rawValue,
            boolean silent) {
        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return SharedCommandUtil.error(source,
                    "Expected a whole number for " + operation.valueName + ".");
        }
        return applySelf(source, operation, skill, value, silent);
    }

    private static void registerReset(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        dispatcher.register(CommandManager.literal("skillreset")
                .requires(source -> permissions.hasPermission(
                                source, PermissionNodes.SKILL_RESET, 2)
                        || permissions.hasPermission(
                                source, PermissionNodes.SKILL_RESET_OTHERS, 2))
                .executes(context -> GenericCommandHelp.show(
                        dispatcher, "skillreset", context.getSource()))
                .then(CommandManager.argument("skill", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, PermissionNodes.SKILL_RESET, 2))
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                SharedCommandUtil.skills(true), builder))
                        .executes(context -> resetSelf(
                                context.getSource(),
                                StringArgumentType.getString(context, "skill"))))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, PermissionNodes.SKILL_RESET_OTHERS, 2))
                        .suggests((context, builder) -> suggestPlayers(
                                context.getSource(), builder))
                        .then(CommandManager.argument("skill", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        SharedCommandUtil.skills(true), builder))
                                .executes(context -> resetOther(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "skill"))))));
    }

    private static CompletableFuture<Suggestions> suggestPlayers(
            ServerCommandSource source,
            SuggestionsBuilder builder) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>(source.getPlayerNames());
        suggestions.addAll(SharedCommandUtil.systems().identities().identities().values());
        return CommandSource.suggestMatching(suggestions, builder);
    }

    private static int applySelf(
            ServerCommandSource source,
            Operation operation,
            String skill,
            int value,
            boolean silent) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception exception) {
            return SharedCommandUtil.error(source, "Specify a player from console.");
        }
        return apply(source, player.getUuid(), player, operation, skill, value, silent);
    }

    private static int applyOther(
            ServerCommandSource source,
            Operation operation,
            String playerName,
            String skill,
            int value,
            boolean silent) {
        var playerId = SharedCommandUtil.playerId(source, playerName);
        if (playerId.isEmpty()) {
            return SharedCommandUtil.error(source, "Player not found: " + playerName);
        }
        UUID id = playerId.orElseThrow();
        return apply(
                source,
                id,
                source.getServer().getPlayerManager().getPlayer(id),
                operation,
                skill,
                value,
                silent);
    }

    private static int apply(
            ServerCommandSource source,
            UUID playerId,
            ServerPlayerEntity onlinePlayer,
            Operation operation,
            String skill,
            int value,
            boolean silent) {
        var administration = SharedCommandUtil.systems().progressionAdmin();
        if (skill.equalsIgnoreCase(ALL)) {
            if (operation == Operation.ADD) {
                administration.addAll(playerId, value);
            } else {
                administration.setAll(playerId, Math.max(0, value));
            }
        } else {
            Optional<SkillDefinition> definition = SharedCommandUtil.skill(skill, false);
            if (definition.isEmpty()) {
                return SharedCommandUtil.error(
                        source, "Unknown or ambiguous skill: " + skill);
            }
            if (operation == Operation.ADD) {
                administration.addLevels(playerId, definition.orElseThrow().id(), value);
            } else {
                administration.setLevel(
                        playerId, definition.orElseThrow().id(), Math.max(0, value));
            }
        }

        if (!silent) {
            String message = operation == Operation.ADD
                    ? "Added " + value + " level(s) in " + skill + "."
                    : "Set " + skill + " level to " + Math.max(0, value) + ".";
            source.sendMessage(Text.literal(
                    message + " Player: " + SharedCommandUtil.playerName(playerId)));
            if (onlinePlayer != null && source.getEntity() != onlinePlayer) {
                onlinePlayer.sendMessage(Text.literal(message));
            }
        }
        return 1;
    }

    private static int resetSelf(ServerCommandSource source, String skill) {
        try {
            return reset(source, source.getPlayerOrThrow().getUuid(), skill);
        } catch (Exception exception) {
            return SharedCommandUtil.error(source, "Specify a player from console.");
        }
    }

    private static int resetOther(
            ServerCommandSource source,
            String player,
            String skill) {
        var playerId = SharedCommandUtil.playerId(source, player);
        return playerId.isEmpty()
                ? SharedCommandUtil.error(source, "Player not found: " + player)
                : reset(source, playerId.orElseThrow(), skill);
    }

    private static int reset(ServerCommandSource source, UUID playerId, String skill) {
        if (skill.equalsIgnoreCase(ALL)) {
            SharedCommandUtil.systems().progressionAdmin().resetAll(playerId);
        } else {
            var definition = SharedCommandUtil.skill(skill, false);
            if (definition.isEmpty()) {
                return SharedCommandUtil.error(source, "Unknown skill: " + skill);
            }
            SharedCommandUtil.systems().progressionAdmin()
                    .setLevel(playerId, definition.orElseThrow().id(), 0);
        }
        return SharedCommandUtil.success(
                source, "Reset " + skill + " for " + SharedCommandUtil.playerName(playerId) + ".");
    }

    private enum Operation {
        ADD("addlevels", PermissionNodes.ADD_LEVELS, PermissionNodes.ADD_LEVELS_OTHERS, "levels"),
        SET("mmoedit", PermissionNodes.MMO_EDIT, PermissionNodes.MMO_EDIT_OTHERS, "level");

        private final String literal;
        private final String self;
        private final String others;
        private final String valueName;

        Operation(String literal, String self, String others, String valueName) {
            this.literal = literal;
            this.self = self;
            this.others = others;
            this.valueName = valueName;
        }
    }
}
