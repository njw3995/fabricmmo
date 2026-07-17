package io.github.njw3995.fabricmmo.core.command;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class StatsTextFormatter {
    private static final List<NamespacedId> GATHERING_SKILLS = List.of(
            CoreSkills.EXCAVATION,
            CoreSkills.FISHING,
            CoreSkills.HERBALISM,
            CoreSkills.MINING,
            CoreSkills.WOODCUTTING);
    private static final List<NamespacedId> COMBAT_SKILLS = List.of(
            CoreSkills.ARCHERY,
            CoreSkills.AXES,
            CoreSkills.CROSSBOWS,
            CoreSkills.MACES,
            CoreSkills.SWORDS,
            CoreSkills.TAMING,
            CoreSkills.TRIDENTS,
            CoreSkills.UNARMED);
    private static final List<NamespacedId> MISC_SKILLS = List.of(
            CoreSkills.ACROBATICS,
            CoreSkills.ALCHEMY,
            CoreSkills.REPAIR,
            CoreSkills.SALVAGE,
            CoreSkills.SMELTING);
    private static final Set<NamespacedId> CORE_DISPLAY_SKILLS = coreDisplaySkills();

    private StatsTextFormatter() {
    }

    public static List<Text> format(
            SkillRegistryView registry,
            Map<NamespacedId, ProgressionSnapshot> progression) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(progression, "progression");

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("[mcMMO] Stats").formatted(Formatting.GREEN));
        lines.add(Text.literal("If you don't have access to a skill it will not be shown here.")
                .formatted(Formatting.DARK_GRAY));

        appendSection(lines, "-=GATHERING SKILLS=-", GATHERING_SKILLS, registry, progression);
        appendSection(lines, "-=COMBAT SKILLS=-", COMBAT_SKILLS, registry, progression);
        appendSection(lines, "-=MISC SKILLS=-", MISC_SKILLS, registry, progression);

        List<NamespacedId> addonSkills = registry.skills().stream()
                .map(SkillDefinition::id)
                .filter(id -> !CORE_DISPLAY_SKILLS.contains(id))
                .sorted()
                .toList();
        appendSection(lines, "-=ADDON SKILLS=-", addonSkills, registry, progression);

        int powerLevel = registry.skills().stream()
                .filter(skill -> !skill.childSkill())
                .map(SkillDefinition::id)
                .map(progression::get)
                .filter(Objects::nonNull)
                .mapToInt(ProgressionSnapshot::level)
                .reduce(0, Math::addExact);
        lines.add(Text.literal("POWER LEVEL: ").formatted(Formatting.DARK_RED)
                .append(Text.literal(Integer.toString(powerLevel)).formatted(Formatting.GREEN)));
        return List.copyOf(lines);
    }

    private static void appendSection(
            List<Text> lines,
            String header,
            List<NamespacedId> skillIds,
            SkillRegistryView registry,
            Map<NamespacedId, ProgressionSnapshot> progression) {
        List<Text> skillLines = skillIds.stream()
                .map(registry::find)
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(SkillDefinition::id))
                .map(skill -> formatSkill(skill, progression.get(skill.id())))
                .filter(Objects::nonNull)
                .toList();
        if (skillLines.isEmpty()) {
            return;
        }
        lines.add(Text.literal(header).formatted(Formatting.GOLD));
        lines.addAll(skillLines);
    }

    private static Text formatSkill(
            SkillDefinition skill,
            ProgressionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        MutableText line = Text.literal(SkillArgumentResolver.displayName(skill.id()) + ": ")
                .append(Text.literal(Integer.toString(snapshot.level())).formatted(Formatting.GREEN));
        if (skill.childSkill()) {
            return line;
        }
        line.append(Text.literal(" XP(").formatted(Formatting.DARK_AQUA))
                .append(Text.literal(Integer.toString(snapshot.xp())).formatted(Formatting.GRAY))
                .append(Text.literal("/").formatted(Formatting.DARK_AQUA));
        if (snapshot.level() >= skill.levelCap()) {
            line.append(Text.literal("Max").formatted(Formatting.GRAY));
        } else {
            line.append(Text.literal(Integer.toString(snapshot.xpToNextLevel()))
                    .formatted(Formatting.GRAY));
        }
        return line.append(Text.literal(")").formatted(Formatting.DARK_AQUA));
    }

    private static Set<NamespacedId> coreDisplaySkills() {
        Set<NamespacedId> ids = new LinkedHashSet<>();
        ids.addAll(GATHERING_SKILLS);
        ids.addAll(COMBAT_SKILLS);
        ids.addAll(MISC_SKILLS);
        return Set.copyOf(ids);
    }
}
