package io.github.njw3995.fabricmmo.core.skill;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistrar;
import io.github.njw3995.fabricmmo.api.skill.SkillCategory;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import java.util.List;
import java.util.Map;

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
        register(registrar, ACROBATICS, SkillCategory.MOVEMENT);
        register(registrar, ALCHEMY, SkillCategory.UTILITY);
        register(registrar, ARCHERY, SkillCategory.COMBAT);
        register(registrar, AXES, SkillCategory.COMBAT);
        register(registrar, CROSSBOWS, SkillCategory.COMBAT);
        register(registrar, EXCAVATION, SkillCategory.GATHERING);
        register(registrar, FISHING, SkillCategory.GATHERING);
        register(registrar, HERBALISM, SkillCategory.GATHERING);
        register(registrar, MACES, SkillCategory.COMBAT);
        register(registrar, MINING, SkillCategory.GATHERING);
        register(registrar, REPAIR, SkillCategory.UTILITY);
        register(registrar, SWORDS, SkillCategory.COMBAT);
        register(registrar, TAMING, SkillCategory.COMBAT);
        register(registrar, TRIDENTS, SkillCategory.COMBAT);
        register(registrar, UNARMED, SkillCategory.COMBAT);
        register(registrar, WOODCUTTING, SkillCategory.GATHERING);
        registrar.registerSkill(new SkillDefinition(
                SALVAGE,
                SkillCategory.CHILD,
                "skill.fabricmmo.salvage",
                1000,
                true,
                List.of(REPAIR, FISHING),
                Map.of("upstream", "PrimarySkillType.SALVAGE")));
        registrar.registerSkill(new SkillDefinition(
                SMELTING,
                SkillCategory.CHILD,
                "skill.fabricmmo.smelting",
                1000,
                true,
                List.of(MINING, REPAIR),
                Map.of("upstream", "PrimarySkillType.SMELTING")));
    }

    private static void register(SkillRegistrar registrar, NamespacedId id, SkillCategory category) {
        registrar.registerSkill(new SkillDefinition(
                id,
                category,
                "skill.fabricmmo." + id.path(),
                1000,
                true,
                List.of(),
                Map.of("upstream", "PrimarySkillType." + id.path().toUpperCase())));
    }

    private static NamespacedId id(String path) {
        return new NamespacedId("fabricmmo", path);
    }
}
