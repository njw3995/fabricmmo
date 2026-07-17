package io.github.njw3995.fabricmmo.core.skill;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistrar;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CoreSkills {
    public static final NamespacedId ACROBATICS = id("acrobatics");
    public static final NamespacedId ALCHEMY = id("alchemy");
    public static final NamespacedId ARCHERY = id("archery");
    public static final NamespacedId AXES = id("axes");
    public static final NamespacedId CROSSBOWS = id("crossbows");
    public static final NamespacedId EXCAVATION = id("excavation");
    public static final NamespacedId FISHING = id("fishing");
    public static final NamespacedId HERBALISM = id("herbalism");
    public static final NamespacedId MACES = id("maces");
    public static final NamespacedId MINING = id("mining");
    public static final NamespacedId REPAIR = id("repair");
    public static final NamespacedId SWORDS = id("swords");
    public static final NamespacedId TAMING = id("taming");
    public static final NamespacedId TRIDENTS = id("tridents");
    public static final NamespacedId UNARMED = id("unarmed");
    public static final NamespacedId WOODCUTTING = id("woodcutting");
    public static final NamespacedId SALVAGE = id("salvage");
    public static final NamespacedId SMELTING = id("smelting");

    private CoreSkills() {
    }

    public static void registerAll(SkillRegistrar registrar) {
        registerAll(registrar, ProgressionSettings.upstreamDefaults());
    }

    public static void registerAll(SkillRegistrar registrar, ProgressionSettings settings) {
        register(registrar, ACROBATICS, SkillCategory.MOVEMENT, settings);
        register(registrar, ALCHEMY, SkillCategory.UTILITY, settings);
        register(registrar, ARCHERY, SkillCategory.COMBAT, settings);
        register(registrar, AXES, SkillCategory.COMBAT, settings);
        register(registrar, CROSSBOWS, SkillCategory.COMBAT, settings);
        register(registrar, EXCAVATION, SkillCategory.GATHERING, settings);
        register(registrar, FISHING, SkillCategory.GATHERING, settings);
        register(registrar, HERBALISM, SkillCategory.GATHERING, settings);
        register(registrar, MACES, SkillCategory.COMBAT, settings);
        register(registrar, MINING, SkillCategory.GATHERING, settings);
        register(registrar, REPAIR, SkillCategory.UTILITY, settings);
        register(registrar, SWORDS, SkillCategory.COMBAT, settings);
        register(registrar, TAMING, SkillCategory.COMBAT, settings);
        register(registrar, TRIDENTS, SkillCategory.COMBAT, settings);
        register(registrar, UNARMED, SkillCategory.COMBAT, settings);
        register(registrar, WOODCUTTING, SkillCategory.GATHERING, settings);
        registrar.registerSkill(new SkillDefinition(
                SALVAGE,
                SkillCategory.CHILD,
                "skill.fabricmmo.salvage",
                Integer.MAX_VALUE,
                true,
                List.of(REPAIR, FISHING),
                Map.of("upstream", "PrimarySkillType.SALVAGE")));
        registrar.registerSkill(new SkillDefinition(
                SMELTING,
                SkillCategory.CHILD,
                "skill.fabricmmo.smelting",
                Integer.MAX_VALUE,
                true,
                List.of(MINING, REPAIR),
                Map.of("upstream", "PrimarySkillType.SMELTING")));
    }

    public static Set<NamespacedId> primarySkillIds() {
        return Set.of(
                ACROBATICS, ALCHEMY, ARCHERY, AXES, CROSSBOWS, EXCAVATION, FISHING,
                HERBALISM, MACES, MINING, REPAIR, SWORDS, TAMING, TRIDENTS, UNARMED,
                WOODCUTTING);
    }

    private static void register(
            SkillRegistrar registrar,
            NamespacedId id,
            SkillCategory category,
            ProgressionSettings settings) {
        registrar.registerSkill(new SkillDefinition(
                id,
                category,
                "skill.fabricmmo." + id.path(),
                settings.levelCap(id),
                true,
                List.of(),
                Map.of("upstream", "PrimarySkillType." + id.path().toUpperCase())));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", path);
    }
}
