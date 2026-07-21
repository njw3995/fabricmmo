package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.server.network.ServerPlayerEntity;

/** Builds the permission-sensitive XP context mcMMO calculates from an online player. */
public final class PlayerProgressionContext {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private PlayerProgressionContext() {
    }

    public static Map<String, String> enrich(
            ServerPlayerEntity player,
            Map<String, String> base,
            ProgressionSettings settings,
            NamespacedId skillId) {
        Map<String, String> context = new HashMap<>(base);
        context.put("xpPerkMultiplier", Double.toString(xpPerkMultiplier(
                player, settings, skillId)));
        context.put("powerLevelSkills", CoreSkills.primarySkillIds().stream()
                .filter(id -> PERMISSIONS.hasPermission(
                        player.getCommandSource(), "mcmmo.skills." + id.path(), true))
                .sorted()
                .map(NamespacedId::toString)
                .collect(Collectors.joining(",")));
        return Map.copyOf(context);
    }

    /** Enriches an external award using every registered permission-visible primary skill. */
    public static Map<String, String> enrich(
            ServerPlayerEntity player,
            Map<String, String> base,
            ProgressionSettings settings,
            NamespacedId skillId,
            SkillRegistryView registry) {
        Map<String, String> context = new HashMap<>(base);
        context.put("xpPerkMultiplier", Double.toString(xpPerkMultiplier(
                player, settings, skillId)));
        context.put("powerLevelSkills", registry.skills().stream()
                .filter(definition -> !definition.childSkill())
                .filter(PlayerProgressionContext::countsTowardPowerLevel)
                .filter(definition -> hasSkillPermission(player, definition))
                .map(SkillDefinition::id)
                .sorted()
                .map(NamespacedId::toString)
                .collect(Collectors.joining(",")));
        return Map.copyOf(context);
    }

    private static boolean countsTowardPowerLevel(SkillDefinition definition) {
        return Boolean.parseBoolean(definition.metadata().getOrDefault("power_level", "true"));
    }

    private static boolean hasSkillPermission(
            ServerPlayerEntity player,
            SkillDefinition definition) {
        String explicit = definition.metadata().getOrDefault("permission", "").trim();
        String permission = !explicit.isBlank()
                ? explicit
                : definition.id().namespace().equals("fabricmmo")
                        ? "mcmmo.skills." + definition.id().path()
                        : "";
        return permission.isBlank() || PERMISSIONS.hasPermission(
                player.getCommandSource(), permission, true);
    }

    static double xpPerkMultiplier(
            ServerPlayerEntity player,
            ProgressionSettings settings,
            NamespacedId skillId) {
        String skill = skillId.path();
        if (hasEither(player, PermissionNodes.XP_CUSTOM_ALL, "mcmmo.perks.xp.customboost." + skill)) {
            return settings.customXpPerkBoost();
        }
        if (hasEither(player, PermissionNodes.XP_QUADRUPLE_ALL, "mcmmo.perks.xp.quadruple." + skill)) {
            return 4.0D;
        }
        if (hasEither(player, PermissionNodes.XP_TRIPLE_ALL, "mcmmo.perks.xp.triple." + skill)) {
            return 3.0D;
        }
        if (hasEither(player, PermissionNodes.XP_150_PERCENT_ALL,
                "mcmmo.perks.xp.150percentboost." + skill)) {
            return 2.5D;
        }
        if (hasEither(player, PermissionNodes.XP_DOUBLE_ALL, "mcmmo.perks.xp.double." + skill)) {
            return 2.0D;
        }
        if (hasEither(player, PermissionNodes.XP_50_PERCENT_ALL,
                "mcmmo.perks.xp.50percentboost." + skill)) {
            return 1.5D;
        }
        if (hasEither(player, PermissionNodes.XP_25_PERCENT_ALL,
                "mcmmo.perks.xp.25percentboost." + skill)) {
            return 1.25D;
        }
        if (hasEither(player, PermissionNodes.XP_10_PERCENT_ALL,
                "mcmmo.perks.xp.10percentboost." + skill)) {
            return 1.1D;
        }
        return 1.0D;
    }

    private static boolean hasEither(
            ServerPlayerEntity player,
            String allPermission,
            String skillPermission) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), allPermission, false)
                || PERMISSIONS.hasPermission(player.getCommandSource(), skillPermission, false);
    }
}
