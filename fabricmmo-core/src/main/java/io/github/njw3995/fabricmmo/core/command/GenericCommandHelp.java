package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Adds concise, permission-aware help to commands that do not define their own help branch. */
final class GenericCommandHelp {
    private static final Set<String> SKILL_COMMANDS = Set.of(
            "excavation", "herbalism", "mining", "woodcutting", "axes", "archery",
            "crossbows", "swords", "taming", "tridents", "unarmed", "acrobatics",
            "repair", "fishing", "smelting", "alchemy", "salvage", "maces");

    private GenericCommandHelp() {
    }

    static void attach(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (UpstreamCommandDefinition definition : UpstreamCommandCatalog.instance().commands()) {
            if (definition.literal().equals("spears")) {
                continue;
            }
            CommandNode<ServerCommandSource> root = dispatcher.getRoot()
                    .getChild(definition.literal());
            if (root == null) {
                continue;
            }
            attachIfMissing(dispatcher, root, definition, "?");
            attachIfMissing(dispatcher, root, definition, "help");
        }
    }

    private static void attachIfMissing(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandNode<ServerCommandSource> root,
            UpstreamCommandDefinition definition,
            String literal) {
        if (root.getChild(literal) != null) {
            return;
        }
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal(literal)
                .executes(context -> show(dispatcher, root, definition, context.getSource()));
        root.addChild(builder.build());
    }

    static int show(
            CommandDispatcher<ServerCommandSource> dispatcher,
            String command,
            ServerCommandSource source) {
        UpstreamCommandDefinition definition = UpstreamCommandCatalog.instance().find(command)
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: " + command));
        CommandNode<ServerCommandSource> root = dispatcher.getRoot().getChild(definition.literal());
        if (root == null) {
            throw new IllegalStateException("Command is not registered: " + definition.literal());
        }
        return show(dispatcher, root, definition, source);
    }

    private static int show(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandNode<ServerCommandSource> root,
            UpstreamCommandDefinition definition,
            ServerCommandSource source) {
        String command = definition.literal();
        source.sendMessage(Text.literal("---- /" + command + " ----")
                .formatted(Formatting.GOLD));
        source.sendMessage(description(command));

        if (!definition.aliases().isEmpty()) {
            source.sendMessage(Text.literal("Aliases: " + definition.aliases().stream()
                    .map(alias -> "/" + alias)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""))
                    .formatted(Formatting.DARK_AQUA));
        }

        List<String> usages = explicitUsages(command, source);
        if (usages.isEmpty()) {
            usages = visibleUsages(dispatcher, root, source, command);
        }
        source.sendMessage(Text.literal("Usage:").formatted(Formatting.YELLOW));
        for (String usage : usages) {
            source.sendMessage(Text.literal("  " + usage).formatted(Formatting.GRAY));
        }

        if (command.equals("mcscoreboard")) {
            source.sendMessage(Text.literal(
                    "This command manages a scoreboard already opened by /mcstats, /mcrank, "
                            + "/mctop, /inspect, /mccooldown, or a skill command; it does not "
                            + "open a scoreboard by itself.")
                    .formatted(Formatting.AQUA));
        } else if (command.equals("xprate")) {
            source.sendMessage(Text.literal(
                    "event-mode is optional and defaults to true: true treats the rate change "
                            + "as an XP event and sends the event announcement; false applies "
                            + "it as a normal rate change without the event banner. It never "
                            + "turns XP off.")
                    .formatted(Formatting.AQUA));
        } else if (command.equals("addlevels") || command.equals("mmoedit")) {
            source.sendMessage(Text.literal(
                    "Append -s to suppress confirmation messages.")
                    .formatted(Formatting.AQUA));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static Text description(String command) {
        var locale = SharedCommandUtil.systems().locale();
        String key = "Commands.Description." + command;
        if (locale.contains(key)) {
            return LegacyText.parse(locale.text(key));
        }
        if (SKILL_COMMANDS.contains(command)) {
            String skill = command.substring(0, 1).toUpperCase(Locale.ROOT)
                    + command.substring(1);
            return LegacyText.parse(locale.text("Commands.Description.Skill", skill));
        }
        return Text.literal(switch (command) {
            case "mmopower" -> "Show your total power level.";
            case "mcmmoreloadlocale" -> "Reload locale_override.properties at runtime.";
            default -> "Show usage and available parameters for this command.";
        }).formatted(Formatting.GRAY);
    }

    static List<String> explicitUsages(String command, ServerCommandSource source) {
        var permissions = SharedCommandUtil.systems().permissions();
        return switch (command) {
            case "addlevels" -> progressionEditUsages(
                    command,
                    "levels",
                    permissions.hasPermission(source, "mcmmo.commands.addlevels", 2),
                    permissions.hasPermission(source, "mcmmo.commands.addlevels.others", 2));
            case "mmoedit" -> progressionEditUsages(
                    command,
                    "level",
                    permissions.hasPermission(source, "mcmmo.commands.mmoedit", 2),
                    permissions.hasPermission(source, "mcmmo.commands.mmoedit.others", 2));
            case "skillreset" -> {
                ArrayList<String> usages = new ArrayList<>();
                if (permissions.hasPermission(source, "mcmmo.commands.skillreset", 2)) {
                    usages.add("/skillreset <skill|all>");
                }
                if (permissions.hasPermission(
                        source, "mcmmo.commands.skillreset.others", 2)) {
                    usages.add("/skillreset <player> <skill|all>");
                }
                yield usages;
            }
            case "xprate" -> {
                ArrayList<String> usages = new ArrayList<>();
                if (permissions.hasPermission(
                        source, "mcmmo.commands.xprate.show", true)) {
                    usages.add("/xprate");
                    usages.add("/xprate show");
                }
                if (permissions.hasPermission(
                        source, "mcmmo.commands.xprate.set", 2)) {
                    usages.add("/xprate <rate> [<event-mode:true|false>]");
                    usages.add(
                            "/xprate <skill|all> <rate> [<event-mode:true|false>]");
                }
                if (permissions.hasPermission(
                        source, "mcmmo.commands.xprate.reset", 2)) {
                    usages.add("/xprate reset");
                }
                yield usages;
            }
            default -> List.of();
        };
    }

    private static List<String> progressionEditUsages(
            String command,
            String valueName,
            boolean self,
            boolean others) {
        ArrayList<String> usages = new ArrayList<>();
        if (self) {
            usages.add("/" + command + " <skill|all> <" + valueName + "> [-s]");
        }
        if (others) {
            usages.add("/" + command + " <player> <skill|all> <" + valueName + "> [-s]");
        }
        return usages;
    }

    private static List<String> visibleUsages(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandNode<ServerCommandSource> root,
            ServerCommandSource source,
            String command) {
        LinkedHashSet<String> usages = new LinkedHashSet<>();
        if (root.getCommand() != null) {
            usages.add("/" + command);
        }
        dispatcher.getSmartUsage(root, source).forEach((child, usage) -> {
            if (!child.getName().equals("?") && !child.getName().equals("help")) {
                usages.add("/" + command + " " + usage);
            }
        });
        if (usages.isEmpty()) {
            usages.add("/" + command);
        }
        return new ArrayList<>(usages);
    }
}
