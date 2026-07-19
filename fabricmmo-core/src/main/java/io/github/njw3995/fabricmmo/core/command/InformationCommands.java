package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.info.SkillPanelService;
import io.github.njw3995.fabricmmo.core.info.SubSkillCatalog;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import java.util.List;
import java.util.Set;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class InformationCommands {
    private static final int PAGE_SIZE = 8;
    private static final Set<String> GENERIC_SKILLS = Set.of(
            "excavation", "herbalism", "woodcutting", "axes", "archery", "crossbows",
            "swords", "taming", "tridents", "unarmed", "acrobatics", "repair", "fishing",
            "smelting", "alchemy", "salvage", "maces");
    private InformationCommands() { }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        var info = dispatcher.register(CommandManager.literal("mmoinfo")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mmoinfo", true))
                .then(CommandManager.argument("subskill", StringArgumentType.greedyString())
                        .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(
                                SubSkillCatalog.instance().entries().stream()
                                        .filter(entry -> entry.applicable())
                                        .map(entry -> entry.lookupName()).toList(), builder))
                        .executes(context -> showSubskill(context.getSource(),
                                StringArgumentType.getString(context, "subskill")))));
        dispatcher.register(CommandManager.literal("mcinfo")
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands.mmoinfo", true))
                .redirect(info));
        for (String skill : GENERIC_SKILLS) registerSkill(dispatcher, permissions, skill);
    }

    static int showHelpPage(ServerCommandSource source, int page) {
        List<UpstreamCommandDefinition> visible = UpstreamCommandCatalog.instance().commands()
                .stream()
                .filter(command -> commandVisibleTo(source, command))
                .toList();
        int pages = Math.max(1, (visible.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 1 || page > pages) return SharedCommandUtil.error(source,
                "Invalid page. Valid pages: 1-" + pages);
        source.sendMessage(Text.literal("---- mcMMO Commands " + page + "/" + pages + " ----")
                .formatted(Formatting.GOLD));
        int start = (page - 1) * PAGE_SIZE;
        for (var command : visible.subList(start, Math.min(start + PAGE_SIZE, visible.size()))) {
            String aliases = command.aliases().isEmpty() ? ""
                    : " (" + String.join(", ", command.aliases()) + ")";
            source.sendMessage(Text.literal("/" + command.literal() + aliases)
                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                    "/" + command.literal()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to prepare this command")))));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static boolean commandVisibleTo(
            ServerCommandSource source,
            UpstreamCommandDefinition command) {
        if (command.literal().equals("spears")) {
            return false;
        }
        var permissions = SharedCommandUtil.systems().permissions();
        return switch (command.literal()) {
            case "mmoxpbar", "mmodebug" -> true;
            case "mcmmo" -> permissions.hasPermission(
                    source, "mcmmo.commands.mcmmo.description", false)
                    || permissions.hasPermission(source, "mcmmo.commands.mcmmo.help", false);
            case "xprate" -> permissions.hasPermission(
                    source, "mcmmo.commands.xprate.show", false)
                    || permissions.hasPermission(source, "mcmmo.commands.xprate", false);
            case "mcconvert" -> permissions.hasPermission(
                    source, "mcmmo.commands.mcconvert.database", false)
                    || permissions.hasPermission(
                            source, "mcmmo.commands.mcconvert.experience", false);
            default -> command.permission()
                    .map(permission -> permissions.hasPermission(source, permission, false))
                    .orElse(true);
        };
    }

    private static void registerSkill(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions, String skill) {
        var root = CommandManager.literal(skill)
                .requires(source -> permissions.hasPermission(source, "mcmmo.commands." + skill, true))
                .executes(context -> showSkill(context.getSource(), skill));
        root.then(CommandManager.literal("?")
                .executes(context -> showSkillGuide(context.getSource(), skill, 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> showSkillGuide(context.getSource(), skill,
                                IntegerArgumentType.getInteger(context, "page")))));
        root.then(CommandManager.literal("keep")
                .executes(context -> keepSkillBoard(context.getSource(), skill)));
        root.then(CommandManager.literal("help")
                .executes(context -> showSkillGuide(context.getSource(), skill, 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> showSkillGuide(context.getSource(), skill,
                                IntegerArgumentType.getInteger(context, "page")))));
        dispatcher.register(root);
    }

    static int keepSkillBoard(ServerCommandSource source, String skillName) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return SharedCommandUtil.error(source, "This command requires a player.");
        }
        var systems = SharedCommandUtil.systems();
        var settings = systems.uiConfiguration();
        if (!settings.scoreboardsEnabled() || !settings.allowKeep()
                || !settings.board(io.github.njw3995.fabricmmo.core.ui.UiSettings.BoardType.SKILL).enabled()) {
            return SharedCommandUtil.error(source,
                    LegacyText.strip(systems.locale().text("Commands.Disabled")));
        }
        var definition = SharedCommandUtil.skill(skillName, true);
        if (definition.isEmpty()) {
            return SharedCommandUtil.error(source, "That skill is not registered.");
        }
        SkillPanelService.Panel panel = systems.skillPanels()
                .panel(source, player.getUuid(), definition.orElseThrow());
        systems.scoreboards().showValues(player,
                panel.scoreboardTitle(), panel.scoreboardRows(), -1);
        systems.scoreboards().keep(player.getUuid());
        return SharedCommandUtil.success(source,
                LegacyText.strip(systems.locale().text("Commands.Scoreboard.Keep")));
    }

    static int showSkill(ServerCommandSource source, String skillName) {
        ServerPlayerEntity player;
        try { player = source.getPlayerOrThrow(); }
        catch (Exception exception) { return SharedCommandUtil.error(source, "This command requires a player."); }
        var definition = SharedCommandUtil.skill(skillName, true);
        if (definition.isEmpty()) return SharedCommandUtil.error(source, "That skill is not registered.");
        SkillPanelService.Panel panel = SharedCommandUtil.systems().skillPanels()
                .panel(source, player.getUuid(), definition.orElseThrow());
        CommandUiDisplay.skill(source,
                panel.scoreboardTitle(), panel.chatLines(), panel.scoreboardRows());
        return 1;
    }

    static int showSkillGuide(ServerCommandSource source, String skill, int page) {
        List<String> lines = SharedServerSystems.require().guides().guide(skill);
        int perPage = 7;
        int pages = Math.max(1, (lines.size() + perPage - 1) / perPage);
        if (page < 1 || page > pages) return SharedCommandUtil.error(source,
                "Invalid guide page. Valid pages: 1-" + pages);
        source.sendMessage(Text.literal("---- " + skill.toUpperCase(java.util.Locale.ROOT)
                + " Guide " + page + "/" + pages + " ----").formatted(Formatting.GOLD));
        for (String line : lines.subList((page - 1) * perPage,
                Math.min(page * perPage, lines.size()))) source.sendMessage(LegacyText.parse(line));
        return 1;
    }

    private static int showSubskill(ServerCommandSource source, String value) {
        var found = SubSkillCatalog.instance().find(value);
        if (found.isEmpty() || !found.orElseThrow().applicable()) return SharedCommandUtil.error(
                source, "That subskill does not exist for Minecraft 1.21.1.");
        var subskill = found.orElseThrow();
        source.sendMessage(Text.literal("---- " + subskill.configName() + " ----")
                .formatted(Formatting.GOLD));
        source.sendMessage(Text.literal("Parent Skill: " + subskill.parentSkill())
                .formatted(Formatting.YELLOW));
        source.sendMessage(Text.literal("Ranks: "
                + (subskill.ranks() == 0 ? "unranked" : subskill.ranks()))
                .formatted(Formatting.GREEN));
        String url = "https://wiki.mcmmo.org/skills/"
                + subskill.parentSkill().toLowerCase(java.util.Locale.ROOT)
                + "#" + subskill.wikiSlug();
        source.sendMessage(Text.literal("Open upstream skill guide")
                .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal(url)))));
        return 1;
    }
}
