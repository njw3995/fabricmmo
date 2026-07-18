package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class XpRateCommands {
    private static final List<String> RATE_SUGGESTIONS = List.of("1.5", "2", "3");

    private XpRateCommands() {
    }

    static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        var root = CommandManager.literal("xprate")
                .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.xprate.show", true)
                        || permissions.hasPermission(
                                source, "mcmmo.commands.xprate.set", 2)
                        || permissions.hasPermission(
                                source, "mcmmo.commands.xprate.reset", 2))
                .executes(context -> show(context.getSource()))
                .then(CommandManager.literal("show")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.xprate.show", true))
                        .executes(context -> show(context.getSource())))
                .then(CommandManager.literal("reset")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.xprate.reset", 2))
                        .executes(context -> reset(context.getSource())))
                .then(CommandManager.argument("rate", DoubleArgumentType.doubleArg(0.000001D))
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.xprate.set", 2))
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                RATE_SUGGESTIONS, builder))
                        .executes(context -> set(
                                context.getSource(),
                                "all",
                                DoubleArgumentType.getDouble(context, "rate"),
                                true))
                        .then(CommandManager.argument(
                                        "event-mode", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        List.of("true", "false"), builder))
                                .executes(context -> setWithEventArgument(
                                        context.getSource(),
                                        "all",
                                        DoubleArgumentType.getDouble(context, "rate"),
                                        StringArgumentType.getString(
                                                context, "event-mode")))))
                .then(CommandManager.argument("skill", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.xprate.set", 2))
                        .suggests(SharedCommandUtil::suggestSkills)
                        .then(CommandManager.argument(
                                        "rate", DoubleArgumentType.doubleArg(0.000001D))
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        RATE_SUGGESTIONS, builder))
                                .executes(context -> set(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "skill"),
                                        DoubleArgumentType.getDouble(context, "rate"),
                                        true))
                                .then(CommandManager.argument(
                                                "event-mode", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                List.of("true", "false"), builder))
                                        .executes(context -> setWithEventArgument(
                                                context.getSource(),
                                                StringArgumentType.getString(
                                                        context, "skill"),
                                                DoubleArgumentType.getDouble(
                                                        context, "rate"),
                                                StringArgumentType.getString(
                                                        context, "event-mode"))))));

        var node = dispatcher.register(root);
        dispatcher.register(CommandManager.literal("mcxprate").redirect(node));
    }

    private static int show(ServerCommandSource source) {
        var snapshot = SharedCommandUtil.systems().xpRates().snapshot();
        source.sendMessage(Text.literal("---- Current XP Rates ----")
                .formatted(Formatting.GOLD));
        source.sendMessage(Text.literal(
                        "Global: " + snapshot.globalRate() + "x"
                                + age(snapshot.globalStartedAt().orElse(null)))
                .formatted(Formatting.YELLOW));
        snapshot.skillRates().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> source.sendMessage(Text.literal(
                        SharedCommandUtil.cap(entry.getKey().path()) + ": "
                                + entry.getValue() + "x"
                                + age(snapshot.skillStartedAt().get(entry.getKey())))));
        if (!snapshot.active()) {
            source.sendMessage(Text.literal("No XP rate event is active.")
                    .formatted(Formatting.GRAY));
        }
        return 1;
    }

    private static String age(Instant instant) {
        return instant == null
                ? ""
                : " (" + Duration.between(instant, Instant.now()).toSeconds() + "s)";
    }

    private static int reset(ServerCommandSource source) {
        boolean eventWasActive = SharedCommandUtil.systems().xpRates().snapshot().globalEvent();
        SharedCommandUtil.systems().xpRates().reset();
        if (eventWasActive) {
            source.getServer().getPlayerManager().broadcast(
                    LegacyText.parse(SharedCommandUtil.systems().locale()
                            .text("Commands.Event.Stop")),
                    false);
        }
        source.getServer().getPlayerManager().broadcast(
                Text.literal("The XP rate has returned to normal."), false);
        return 1;
    }

    private static int setWithEventArgument(
            ServerCommandSource source,
            String target,
            double rate,
            String eventArgument) {
        if (!eventArgument.equalsIgnoreCase("true")
                && !eventArgument.equalsIgnoreCase("false")) {
            return SharedCommandUtil.error(
                    source, "event-mode must be true or false.");
        }
        return set(source, target, rate, Boolean.parseBoolean(eventArgument));
    }

    private static int set(
            ServerCommandSource source,
            String target,
            double rate,
            boolean announceEvent) {
        var service = SharedCommandUtil.systems().xpRates();
        var change = target.equalsIgnoreCase("all")
                ? service.setGlobal(rate, announceEvent)
                : SharedCommandUtil.skill(target, false)
                        .map(skill -> service.setSkill(skill.id(), rate, announceEvent))
                        .orElse(null);
        if (change == null) {
            return SharedCommandUtil.error(source, "Unknown skill: " + target);
        }
        if (!change.applied()) {
            return SharedCommandUtil.error(source, change.detail());
        }

        String scope = target.equalsIgnoreCase("all")
                ? "global"
                : SharedCommandUtil.cap(target);
        if (announceEvent) {
            source.getServer().getPlayerManager().broadcast(
                    LegacyText.parse(SharedCommandUtil.systems().locale()
                            .text("Commands.Event.Start")),
                    false);
        }
        source.getServer().getPlayerManager().broadcast(
                Text.literal("XP rate for " + scope + " is now " + rate + "x."),
                false);

        if (!announceEvent && !target.equalsIgnoreCase("all")) {
            var snapshot = service.snapshot();
            if (snapshot.globalEvent() && snapshot.globalRate() > rate) {
                source.sendMessage(LegacyText.parse(SharedCommandUtil.systems().locale().text(
                        "Commands.xprate.skill.globalApplies",
                        SharedCommandUtil.cap(target),
                        snapshot.globalRate())));
            }
        }
        if (change.clearedSkillRates() > 0) {
            source.sendMessage(Text.literal(
                    "Cleared " + change.clearedSkillRates() + " lower per-skill rates."));
        }
        return 1;
    }
}
