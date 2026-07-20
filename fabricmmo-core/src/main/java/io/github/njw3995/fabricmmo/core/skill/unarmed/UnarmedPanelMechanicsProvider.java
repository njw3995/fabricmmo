package io.github.njw3995.fabricmmo.core.skill.unarmed;

import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Exact mcMMO 2.3.000 UnarmedCommand statsDisplay ordering and wording. */
public final class UnarmedPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final UnarmedSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public UnarmedPanelMechanicsProvider(
            MinecraftServer server,
            UnarmedSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.UNARMED_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (settings.arrowDeflectRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.UNARMED_ARROW_DEFLECT, true))) {
            rows.add(new MechanicRow(
                    locale.text("Unarmed.SubSkill.ArrowDeflect.Stat"),
                    chance(settings.arrowDeflectChancePercent(level, false),
                            settings.arrowDeflectChancePercent(level, true), lucky)));
        }
        if (settings.berserkRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.UNARMED_BERSERK, true))) {
            int baseDuration = settings.berserkDurationSeconds(level);
            int enduranceBonus = player == null ? 0 : UnarmedPerks.activationBonusSeconds(
                    player.getCommandSource(), permissions);
            String duration = Integer.toString(baseDuration);
            if (enduranceBonus > 0) {
                duration += locale.text(
                        "Perks.ActivationTime.Bonus", baseDuration + enduranceBonus);
            }
            rows.add(new MechanicRow(
                    locale.text("Unarmed.SubSkill.Berserk.Stat"), duration));
        }
        if (settings.disarmRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.UNARMED_DISARM, true))) {
            rows.add(new MechanicRow(
                    locale.text("Unarmed.SubSkill.Disarm.Stat"),
                    chance(settings.disarmChancePercent(level, false),
                            settings.disarmChancePercent(level, true), lucky)));
        }
        if (settings.steelArmRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.UNARMED_STEEL_ARM_STYLE, true))) {
            rows.add(new MechanicRow(
                    locale.text("Unarmed.Ability.Bonus.0"),
                    locale.text("Unarmed.Ability.Bonus.1", settings.steelArmDamage(level))));
        }
        if (settings.ironGripRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.UNARMED_IRON_GRIP, true))) {
            rows.add(new MechanicRow(
                    locale.text("Unarmed.SubSkill.IronGrip.Stat"),
                    chance(settings.ironGripChancePercent(level, false),
                            settings.ironGripChancePercent(level, true), lucky)));
        }
        if (settings.limitBreakRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.UNARMED_LIMIT_BREAK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Unarmed.SubSkill.UnarmedLimitBreak.Stat"),
                    Integer.toString(UnarmedDamage.limitBreakDamage(
                            settings.limitBreakRank(level), 1000))));
        }
        return List.copyOf(rows);
    }

    private String chance(double normal, double luckyValue, boolean lucky) {
        String value = percent(normal);
        return lucky ? value + locale.text("Perks.Lucky.Bonus", percent(luckyValue)) : value;
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
