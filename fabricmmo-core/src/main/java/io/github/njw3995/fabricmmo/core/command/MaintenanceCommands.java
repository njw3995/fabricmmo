package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

final class MaintenanceCommands {
    private MaintenanceCommands() { }

    static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        dispatcher.register(CommandManager.literal("mmoshowdb")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mmoshowdb", 2))
                .executes(context -> SharedCommandUtil.success(context.getSource(),
                        "Storage backend: " + SharedCommandUtil.systems().maintenance().backendName())));

        dispatcher.register(CommandManager.literal("mcconvert")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mcconvert", 2))
                .then(CommandManager.literal("database")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.mcconvert.database", 2))
                        .then(CommandManager.argument("previousBackend", StringArgumentType.word())
                                .suggests((context, builder) -> net.minecraft.command.CommandSource
                                        .suggestMatching(java.util.List.of("flatfile", "mysql"), builder))
                                .executes(context -> convertDatabase(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "previousBackend")))))
                .then(CommandManager.literal("db")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.mcconvert.database", 2))
                        .then(CommandManager.argument("previousBackend", StringArgumentType.word())
                                .suggests((context, builder) -> net.minecraft.command.CommandSource
                                        .suggestMatching(java.util.List.of("flatfile", "mysql"), builder))
                                .executes(context -> convertDatabase(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "previousBackend")))))
                .then(experienceConversion("experience", permissions))
                .then(experienceConversion("xp", permissions))
                .then(experienceConversion("exp", permissions)));

        dispatcher.register(CommandManager.literal("mcremove")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mcremove", 2))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(SharedCommandUtil::suggestPlayers)
                        .executes(context -> remove(
                                context.getSource(), StringArgumentType.getString(context, "player")))));

        dispatcher.register(CommandManager.literal("mcpurge")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mcpurge", 2))
                .executes(context -> purge(context.getSource())));

        var reload = dispatcher.register(CommandManager.literal("mcmmoreloadlocale")
                .requires(source -> permissions.hasPermission(
                        source, "mcmmo.commands.mcmmoreloadlocale", 2))
                .executes(context -> reloadLocale(context.getSource())));
        dispatcher.register(CommandManager.literal("mcreloadlocale").redirect(reload));
    }

    private static int reloadLocale(ServerCommandSource source) {
        try {
            var systems = SharedCommandUtil.systems();
            var result = systems.locale().reload(
                    systems.configDirectory().resolve("locales/locale_override.properties"));
            return SharedCommandUtil.success(source,
                    "Reloaded locale overrides: " + result.overrideCount()
                            + " entries (generation " + result.generation() + ").");
        } catch (IOException exception) {
            return SharedCommandUtil.error(source,
                    "Unable to reload locale overrides: " + exception.getMessage());
        }
    }



    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource>
            experienceConversion(String literal, CommandPermissionService permissions) {
        return CommandManager.literal(literal)
                .requires(source -> permissions.hasPermission(
                        source, "mcmmo.commands.mcconvert.experience", 2))
                .then(CommandManager.argument("formula", StringArgumentType.word())
                        .suggests((context, builder) -> net.minecraft.command.CommandSource
                                .suggestMatching(java.util.List.of("linear", "exponential"), builder))
                        .executes(context -> convertExperience(
                                context.getSource(),
                                StringArgumentType.getString(context, "formula"))));
    }

    private static int convertExperience(ServerCommandSource source, String value) {
        final FormulaType target;
        try {
            target = FormulaType.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            source.sendError(LegacyText.parse(SharedCommandUtil.systems().locale().text(
                    "Commands.mcconvert.Experience.Invalid")));
            return 0;
        }
        final FormulaType previous;
        try {
            previous = SharedCommandUtil.systems().conversion().previousFormula();
        } catch (IOException exception) {
            return SharedCommandUtil.error(
                    source, "Unable to read the previous XP formula: " + exception.getMessage());
        }
        if (previous == target) {
            source.sendMessage(LegacyText.parse(SharedCommandUtil.systems().locale().text(
                    "Commands.mcconvert.Experience.Same", target)));
            return 1;
        }
        source.sendMessage(LegacyText.parse(SharedCommandUtil.systems().locale().text(
                "Commands.mcconvert.Experience.Start", previous, target)));
        CompletableFuture.supplyAsync(() -> {
            try {
                return SharedCommandUtil.systems().conversion().convert(target);
            } catch (IOException | RuntimeException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((result, failure) -> source.getServer().execute(() -> {
            if (failure != null) {
                source.sendError(net.minecraft.text.Text.literal(
                        "Experience conversion failed: " + rootMessage(failure)));
                return;
            }
            source.sendMessage(LegacyText.parse(SharedCommandUtil.systems().locale().text(
                    "Commands.mcconvert.Experience.Finish", result.targetFormula())));
            if (result.backup() != null) {
                source.sendMessage(net.minecraft.text.Text.literal(
                        "Progression backup: " + result.backup()));
            }
        }));
        return 1;
    }

    private static int convertDatabase(ServerCommandSource source, String previousBackend) {
        String active = SharedCommandUtil.systems().maintenance().backendName();
        if (normal(previousBackend).equals(active)) {
            return SharedCommandUtil.error(source,
                    "The previous and active database types are both " + active + '.');
        }
        SharedCommandUtil.success(source,
                "Starting progression conversion from " + previousBackend + " to " + active + ".");
        CompletableFuture.supplyAsync(() -> {
            try {
                return SharedCommandUtil.systems().maintenance().convert(previousBackend, active);
            } catch (IOException | RuntimeException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((result, failure) -> source.getServer().execute(() -> {
            if (failure != null) {
                source.sendError(net.minecraft.text.Text.literal(
                        "Database conversion failed: " + rootMessage(failure)));
                return;
            }
            String backup = result.backup() == null ? "" : " Backup: " + result.backup();
            source.sendMessage(net.minecraft.text.Text.literal(result.detail() + backup));
        }));
        return 1;
    }

    private static int purge(ServerCommandSource source) {
        SharedCommandUtil.success(source, "Starting powerless and inactive player purge.");
        CompletableFuture.supplyAsync(() -> {
            try {
                var powerless = SharedCommandUtil.systems().maintenance().purgePowerless();
                int cutoff = FlatYamlConfig.load(
                        SharedCommandUtil.systems().configDirectory().resolve("config.yml"))
                        .integer("Database_Purging.Old_User_Cutoff", 6);
                var old = SharedCommandUtil.systems().maintenance().purgeOldUsers(cutoff);
                return new int[]{powerless.removedPlayers(), old.removedPlayers()};
            } catch (IOException | RuntimeException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((counts, failure) -> source.getServer().execute(() -> {
            if (failure != null) {
                source.sendError(net.minecraft.text.Text.literal(
                        "Player purge failed: " + rootMessage(failure)));
            } else {
                source.sendMessage(net.minecraft.text.Text.literal(
                        "Purge complete. Removed " + counts[0] + " powerless and "
                                + counts[1] + " inactive player records."));
            }
        }));
        return 1;
    }

    private static int remove(ServerCommandSource source, String name) {
        var id = SharedCommandUtil.playerId(source, name);
        if (id.isEmpty()) return SharedCommandUtil.error(source, "Player not found: " + name);
        try {
            if (!SharedCommandUtil.systems().maintenance().delete(id.orElseThrow())) {
                return SharedCommandUtil.error(source, "No stored progression exists for " + name + '.');
            }
            return SharedCommandUtil.success(source, "Removed stored progression for " + name + '.');
        } catch (IOException exception) {
            return SharedCommandUtil.error(source, "Unable to remove player data: " + exception.getMessage());
        }
    }

    private static String normal(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "flat", "flatfile", "properties" -> "flatfile";
            case "sql", "mysql", "mariadb" -> "mysql";
            default -> value.toLowerCase(java.util.Locale.ROOT);
        };
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
