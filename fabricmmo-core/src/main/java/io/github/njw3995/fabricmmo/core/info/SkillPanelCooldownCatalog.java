package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.ability.AbilityCooldownService;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Skill-to-ability mapping for upstream-shaped skill scoreboards. */
public final class SkillPanelCooldownCatalog {
    private static final int LEGACY_SCOREBOARD_LIMIT = 16;
    private static final int LEGACY_COLOR_CODE_LENGTH = 2;
    private static final int LEGACY_DOTTED_VISIBLE_LENGTH = 12;

    private final AbilityCooldownService cooldowns;
    private final LocaleService locale;
    private final boolean abilityNames;
    private final Map<NamespacedId, List<Entry>> entries = new ConcurrentHashMap<>();

    public SkillPanelCooldownCatalog(
            AbilityCooldownService cooldowns,
            LocaleService locale,
            boolean abilityNames) {
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.abilityNames = abilityNames;
        registerUpstreamCoreEntries();
    }

    /** Registers an addon ability using the same aqua skill-board formatting as upstream. */
    public void register(NamespacedId skillId, NamespacedId abilityId, String label) {
        register(skillId, abilityId, label, Formatting.AQUA);
    }

    public List<CooldownRow> rows(NamespacedId skillId, UUID playerId) {
        Map<NamespacedId, Integer> remaining = cooldowns.remaining(playerId);
        return entries.getOrDefault(skillId, List.of()).stream()
                .map(entry -> new CooldownRow(
                        entry.label(), remaining.getOrDefault(entry.abilityId(), 0)))
                .toList();
    }

    private void registerUpstreamCoreEntries() {
        registerLocale(CoreSkills.ARCHERY, id("explosive_shot"),
                "Archery.SubSkill.ExplosiveShot.Name", Formatting.AQUA);
        registerLocale(CoreSkills.AXES, id("skull_splitter"),
                "Axes.SubSkill.SkullSplitter.Name", Formatting.AQUA);
        registerLocale(CoreSkills.CROSSBOWS, id("super_shotgun"),
                "Placeholder", Formatting.AQUA);
        registerLocale(CoreSkills.EXCAVATION, id("giga_drill_breaker"),
                "Excavation.SubSkill.GigaDrillBreaker.Name", Formatting.AQUA);
        registerLocale(CoreSkills.HERBALISM, id("green_terra"),
                "Herbalism.SubSkill.GreenTerra.Name", Formatting.AQUA);
        registerLocale(CoreSkills.MACES, id("maces_super_ability"),
                "Placeholder", Formatting.AQUA);
        registerLocale(CoreSkills.MINING, id("super_breaker"),
                "Mining.SubSkill.SuperBreaker.Name", Formatting.AQUA);
        registerLocale(CoreSkills.MINING, id("blast_mining"),
                "Mining.SubSkill.BlastMining.Name", Formatting.BLUE);
        registerLocale(CoreSkills.SWORDS, id("serrated_strikes"),
                "Swords.SubSkill.SerratedStrikes.Name", Formatting.AQUA);
        registerLocale(CoreSkills.TRIDENTS, id("tridents_super_ability"),
                "Placeholder", Formatting.AQUA);
        registerLocale(CoreSkills.UNARMED, id("berserk"),
                "Unarmed.SubSkill.Berserk.Name", Formatting.AQUA);
        registerLocale(CoreSkills.WOODCUTTING, id("tree_feller"),
                "Woodcutting.SubSkill.TreeFeller.Name", Formatting.AQUA);
    }

    private void registerLocale(
            NamespacedId skillId,
            NamespacedId abilityId,
            String localeKey,
            Formatting color) {
        String visibleName = abilityNames
                ? locale.text(localeKey)
                : stripLegacy(locale.text("Scoreboard.Misc.Ability"));
        register(skillId, abilityId, visibleName, color);
    }

    private void register(
            NamespacedId skillId,
            NamespacedId abilityId,
            String label,
            Formatting color) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(color, "color");
        Text formatted = Text.literal(shortenColoredLabel(label)).formatted(color);
        entries.compute(skillId, (ignored, current) -> {
            ArrayList<Entry> updated = new ArrayList<>(current == null ? List.of() : current);
            updated.removeIf(entry -> entry.abilityId().equals(abilityId));
            updated.add(new Entry(abilityId, formatted));
            return List.copyOf(updated);
        });
    }

    private static String shortenColoredLabel(String value) {
        if (value.length() + LEGACY_COLOR_CODE_LENGTH > LEGACY_SCOREBOARD_LIMIT) {
            return value.substring(0, Math.min(LEGACY_DOTTED_VISIBLE_LENGTH, value.length())) + "..";
        }
        return value;
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

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", path);
    }

    private record Entry(NamespacedId abilityId, Text label) { }

    public record CooldownRow(Text label, int seconds) { }
}
