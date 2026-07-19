package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.ui.ScoreboardValueLine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Shared upstream-shaped skill panel for every applicable core and addon skill. */
public final class SkillPanelService {
    private static final int SUBSKILLS_PER_LINE = 3;

    private final FabricMmoApi api;
    private final SkillPanelMechanicsCatalog mechanics;
    private final SubSkillRankCatalog ranks;
    private final CommandPermissionService permissions;
    private final SkillPanelCooldownCatalog cooldowns;
    private final LocaleService locale;
    private final Map<String, Formatting> scoreboardSkillColors;
    private final boolean scoreboardRainbows;
    private final boolean blankLinesAboveHeader;

    public SkillPanelService(
            FabricMmoApi api,
            SkillPanelMechanicsCatalog mechanics,
            SubSkillRankCatalog ranks,
            CommandPermissionService permissions,
            SkillPanelCooldownCatalog cooldowns,
            LocaleService locale,
            boolean scoreboardRainbows,
            boolean blankLinesAboveHeader) {
        this.api = Objects.requireNonNull(api, "api");
        this.mechanics = Objects.requireNonNull(mechanics, "mechanics");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.scoreboardRainbows = scoreboardRainbows;
        this.blankLinesAboveHeader = blankLinesAboveHeader;
        this.scoreboardSkillColors = buildSkillColors(scoreboardRainbows);
    }

    public Panel panel(ServerCommandSource source, UUID playerId, SkillDefinition skill) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skill, "skill");
        var progress = api.progression().query(playerId, skill.id());
        ArrayList<Text> chat = new ArrayList<>();
        ArrayList<ScoreboardValueLine> board = new ArrayList<>();
        String name = headerSkillName(skill.id().path());
        Text scoreboardTitle = skillLabel(skill.id().path());

        if (blankLinesAboveHeader) {
            chat.add(Text.empty());
            chat.add(Text.empty());
        }
        chat.add(localeText("Skills.Overhaul.Header", name));
        if (skill.childSkill()) {
            chat.add(localeText(
                    "Commands.XPGain.Overhaul",
                    locale.text("Commands.XPGain.Child")));
            MutableText parentMessage = Text.empty();
            for (int index = 0; index < skill.parents().size(); index++) {
                var parent = skill.parents().get(index);
                var parentProgress = api.progression().query(playerId, parent);
                board.add(new ScoreboardValueLine(
                        skillLabel(parent.path()), parentProgress.level()));
                if (index > 0) {
                    parentMessage.append(Text.literal(", ").formatted(Formatting.GRAY));
                }
                parentMessage.append(Text.literal(headerSkillName(parent.path()))
                        .formatted(Formatting.GREEN))
                        .append(Text.literal("(").formatted(Formatting.GOLD))
                        .append(Text.literal("Lv.").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal(Integer.toString(parentProgress.level()))
                                .formatted(Formatting.YELLOW))
                        .append(Text.literal(")").formatted(Formatting.GOLD));
            }
            chat.add(Text.literal("Child Lv.").formatted(Formatting.DARK_AQUA)
                    .append(Text.literal(" " + progress.level()).formatted(Formatting.YELLOW))
                    .append(Text.literal(": ").formatted(Formatting.DARK_AQUA))
                    .append(parentMessage));
            board.add(new ScoreboardValueLine(scoreboardLabel(
                    "Scoreboard.Misc.Level", Formatting.DARK_AQUA), progress.level()));
        } else {
            String xpGainKey = "Commands.XPGain." + cap(skill.id().path());
            String xpGain = locale.contains(xpGainKey)
                    ? locale.text(xpGainKey)
                    : "See /" + skill.id().path() + " ?";
            chat.add(localeText("Commands.XPGain.Overhaul", xpGain));
            chat.add(localeText(
                    "Effects.Level.Overhaul",
                    progress.level(),
                    progress.xp(),
                    progress.xpToNextLevel()));
            board.add(new ScoreboardValueLine(scoreboardLabel(
                    "Scoreboard.Misc.Level", Formatting.DARK_AQUA), progress.level()));
            board.add(new ScoreboardValueLine(scoreboardLabel(
                    "Scoreboard.Misc.CurrentXP", Formatting.GREEN), progress.xp()));
            board.add(new ScoreboardValueLine(scoreboardLabel(
                    "Scoreboard.Misc.RemainingXP", Formatting.YELLOW),
                    Math.max(0, progress.xpToNextLevel() - progress.xp())));
        }

        List<SubSkillInfo> subskills = SubSkillCatalog.instance().entries().stream()
                .filter(SubSkillInfo::applicable)
                .filter(info -> info.parentSkill().equalsIgnoreCase(skill.id().path()))
                .filter(info -> permissions.hasPermission(
                        source, permissionNode(info), 2))
                .toList();
        if (!subskills.isEmpty()) {
            chat.add(localeText(
                    "Skills.Overhaul.Header",
                    locale.text("Effects.SubSkills.Overhaul")));
            ArrayList<Text> buttons = new ArrayList<>();
            for (SubSkillInfo subskill : subskills) {
                buttons.add(subskillButton(subskill, progress.level()));
            }
            for (int start = 0; start < buttons.size(); start += SUBSKILLS_PER_LINE) {
                MutableText line = Text.empty();
                int end = Math.min(start + SUBSKILLS_PER_LINE, buttons.size());
                for (int index = start; index < end; index++) {
                    line.append(Text.literal("@").formatted(Formatting.YELLOW));
                    line.append(buttons.get(index));
                    line.append(Text.literal(" "));
                }
                chat.add(line);
            }
        }

        List<SkillPanelMechanicsProvider.MechanicRow> mechanicRows =
                mechanics.provider(skill.id()).rows(playerId, progress.level());
        if (!mechanicRows.isEmpty()) {
            chat.add(localeText(
                    "Skills.Overhaul.Header",
                    locale.text("Commands.Stats.Self.Overhaul")));
            for (SkillPanelMechanicsProvider.MechanicRow row : mechanicRows) {
                chat.add(localeText(row.templateKey(), row.arguments()));
            }
        }

        for (SkillPanelCooldownCatalog.CooldownRow cooldown : cooldowns.rows(skill.id(), playerId)) {
            board.add(new ScoreboardValueLine(cooldown.label(), cooldown.seconds()));
        }

        chat.add(localeText(
                "Guides.Available",
                name,
                name.toLowerCase(Locale.ENGLISH)));
        return new Panel(scoreboardTitle, List.copyOf(chat), List.copyOf(board));
    }

    private Text subskillButton(SubSkillInfo subskill, int level) {
        int rank = ranks.rank(subskill, level);
        boolean unlocked = rank != 0;
        String displayName = subskillDisplayName(subskill);
        if (!unlocked) {
            int unlockLevel = ranks.unlockLevel(subskill);
            return LegacyText.parse(locale.text("JSON.Hover.Mystery", unlockLevel)).copy()
                    .setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/mmoinfo ???"))
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Unlocks at level " + unlockLevel)))
                            .withInsertion(displayName));
        }

        String localeKey = subskill.ranks() > 1 && rank >= subskill.ranks()
                ? "JSON.Hover.MaxRankSkillName"
                : "JSON.Hover.SkillName";
        return LegacyText.parse(locale.text(localeKey, displayName)).copy()
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/mmoinfo " + subskill.lookupName()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click for " + displayName + " details")))
                        .withInsertion(displayName));
    }

    private String subskillDisplayName(SubSkillInfo subskill) {
        String key = cap(subskill.parentSkill()) + ".SubSkill."
                + subskill.configName() + ".Name";
        return locale.contains(key) ? locale.text(key) : splitCamelCase(subskill.configName());
    }

    private Text localeText(String key, Object... args) {
        return LegacyText.parse(locale.text(key, args));
    }

    private static String permissionNode(SubSkillInfo subskill) {
        return "mcmmo.ability."
                + subskill.parentSkill().toLowerCase(Locale.ROOT)
                + '.'
                + subskill.configName().toLowerCase(Locale.ROOT);
    }

    private String headerSkillName(String path) {
        String key = cap(path) + ".SkillName";
        return locale.contains(key) ? locale.text(key) : cap(path).toUpperCase(Locale.ROOT);
    }

    private Text skillLabel(String path) {
        String visible = shortenSkillLabel(headerSkillName(path), scoreboardRainbows);
        return Text.literal(visible).formatted(
                scoreboardSkillColors.getOrDefault(path.toLowerCase(Locale.ROOT), Formatting.GREEN));
    }

    static String shortenSkillLabel(String value, boolean rainbows) {
        int legacyLength = value.length() + 2;
        if (legacyLength <= 16) {
            return value;
        }
        if (rainbows) {
            return value.substring(0, Math.min(14, value.length()));
        }
        return value.substring(0, Math.min(12, value.length())) + "..";
    }

    private Text scoreboardLabel(String key, Formatting fallbackColor) {
        String raw = locale.text(key);
        String visible = stripLegacy(raw);
        Formatting color = leadingFormatting(raw, fallbackColor);
        return Text.literal(visible).formatted(color);
    }

    private static Formatting leadingFormatting(String value, Formatting fallback) {
        if (value.length() >= 2 && value.charAt(0) == '&') {
            Formatting formatting = Formatting.byCode(value.charAt(1));
            if (formatting != null && formatting.isColor()) {
                return formatting;
            }
        }
        return fallback;
    }

    private static String stripLegacy(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '&' && index + 1 < value.length()
                    && Formatting.byCode(value.charAt(index + 1)) != null) {
                index++;
            } else {
                result.append(value.charAt(index));
            }
        }
        return result.toString();
    }

    private static Map<String, Formatting> buildSkillColors(boolean rainbows) {
        List<String> upstreamOrder = List.of(
                "acrobatics", "alchemy", "archery", "axes", "crossbows", "excavation",
                "fishing", "herbalism", "maces", "mining", "repair", "salvage",
                "smelting", "spears", "swords", "taming", "tridents", "unarmed",
                "woodcutting");
        if (!rainbows) {
            LinkedHashMap<String, Formatting> colors = new LinkedHashMap<>();
            upstreamOrder.forEach(skill -> colors.put(skill, Formatting.GREEN));
            return Map.copyOf(colors);
        }
        ArrayList<Formatting> palette = new ArrayList<>(List.of(
                Formatting.WHITE, Formatting.YELLOW, Formatting.LIGHT_PURPLE,
                Formatting.RED, Formatting.AQUA, Formatting.GREEN, Formatting.DARK_GRAY,
                Formatting.BLUE, Formatting.DARK_PURPLE, Formatting.DARK_RED,
                Formatting.DARK_AQUA, Formatting.DARK_GREEN, Formatting.DARK_BLUE));
        Collections.shuffle(palette);
        LinkedHashMap<String, Formatting> colors = new LinkedHashMap<>();
        for (int index = 0; index < upstreamOrder.size(); index++) {
            colors.put(upstreamOrder.get(index), palette.get(index % palette.size()));
        }
        return Map.copyOf(colors);
    }

    private static String splitCamelCase(String value) {
        return value.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    }

    private static String cap(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public record Panel(
            Text scoreboardTitle,
            List<Text> chatLines,
            List<ScoreboardValueLine> scoreboardRows) { }
}
