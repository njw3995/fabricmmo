package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.command.LegacyText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Shared non-Mining skill panel assembled from registered skill and pinned subskill metadata. */
public final class SkillPanelService {
    private final FabricMmoApi api;
    private final SkillPanelMechanicsCatalog mechanics;

    public SkillPanelService(FabricMmoApi api) {
        this(api, new SkillPanelMechanicsCatalog());
    }

    public SkillPanelService(FabricMmoApi api, SkillPanelMechanicsCatalog mechanics) {
        this.api = api;
        this.mechanics = mechanics;
    }

    public Panel panel(UUID playerId, SkillDefinition skill) {
        var progress = api.progression().query(playerId, skill.id());
        int power = api.progression().queryAll(playerId).entrySet().stream()
                .filter(entry -> api.skillRegistry().find(entry.getKey())
                        .map(definition -> !definition.childSkill()).orElse(false))
                .mapToInt(entry -> entry.getValue().level()).sum();
        ArrayList<Text> chat = new ArrayList<>();
        ArrayList<Text> board = new ArrayList<>();
        String name = cap(skill.id().path());
        chat.add(Text.literal("---- " + name.toUpperCase(Locale.ROOT) + " ----")
                .formatted(Formatting.GOLD));
        if (skill.childSkill()) {
            chat.add(Text.literal("Level: " + progress.level())
                    .formatted(Formatting.GREEN));
            String parents = skill.parents().stream()
                    .map(parent -> cap(parent.path()))
                    .collect(java.util.stream.Collectors.joining(" + "));
            chat.add(Text.literal("Derived from: " + parents)
                    .formatted(Formatting.YELLOW));
            board.add(Text.literal("Level: " + progress.level()));
            board.add(Text.literal("Parents: " + parents));
        } else {
            chat.add(Text.literal("Level: " + progress.level() + "  XP: " + progress.xp()
                    + "/" + progress.xpToNextLevel()).formatted(Formatting.GREEN));
            board.add(Text.literal("Level: " + progress.level()));
            board.add(Text.literal("XP: " + progress.xp() + "/" + progress.xpToNextLevel()));
        }
        chat.add(Text.literal("Power Level: " + power + "  Category: " + skill.category())
                .formatted(Formatting.YELLOW));
        board.add(Text.literal("Power: " + power));

        List<SkillPanelMechanicsProvider.MechanicRow> mechanicRows =
                mechanics.provider(skill.id()).rows(playerId, progress.level());
        if (!mechanicRows.isEmpty()) {
            chat.add(Text.literal("Mechanics:").formatted(Formatting.GOLD));
            for (SkillPanelMechanicsProvider.MechanicRow row : mechanicRows) {
                chat.add(Text.literal("  " + row.label() + ": ").formatted(Formatting.YELLOW)
                        .append(Text.literal(row.value()).formatted(Formatting.GRAY)));
                board.add(Text.literal(row.label() + ": " + row.value()));
            }
        }

        List<SubSkillInfo> subskills = SubSkillCatalog.instance().entries().stream()
                .filter(SubSkillInfo::applicable)
                .filter(info -> info.parentSkill().equalsIgnoreCase(skill.id().path()))
                .sorted(Comparator.comparing(SubSkillInfo::configName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (!subskills.isEmpty()) {
            chat.add(Text.literal("Subskills:").formatted(Formatting.GOLD));
            for (SubSkillInfo subskill : subskills) {
                Text line = Text.literal("  " + subskill.configName()
                                + (subskill.ranks() > 0 ? " (" + subskill.ranks() + " ranks)" : ""))
                        .setStyle(Style.EMPTY.withColor(Formatting.YELLOW)
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/mmoinfo " + subskill.lookupName()))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("Click for " + subskill.configName() + " details"))));
                chat.add(line);
            }
            board.add(Text.literal("Subskills: " + subskills.size()));
        }
        chat.add(Text.literal("Use /" + skill.id().path() + " ? for the full guide.")
                .formatted(Formatting.GRAY));
        return new Panel(List.copyOf(chat), List.copyOf(board));
    }

    private static String cap(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public record Panel(List<Text> chatLines, List<Text> scoreboardLines) { }
}
