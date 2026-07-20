package io.github.njw3995.fabricmmo.core.skill.swords;

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

/** Exact mcMMO 2.3.000 SwordsCommand statsDisplay ordering and wording. */
public final class SwordsPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final SwordsSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public SwordsPanelMechanicsProvider(
            MinecraftServer server,
            SwordsSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.SWORDS_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>();
        if (settings.counterRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.SWORDS_COUNTER_ATTACK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Swords.SubSkill.CounterAttack.Stat"),
                    chance(settings.counterChancePercent(level, false),
                            settings.counterChancePercent(level, true), lucky)));
        }
        int ruptureRank = settings.ruptureRank(level);
        if (ruptureRank > 0
                && (!inspect || allowed(player, PermissionNodes.SWORDS_RUPTURE, true))) {
            double ruptureChance = settings.ruptureChancePercent(ruptureRank, false);
            rows.add(new MechanicRow(
                    locale.text("Swords.SubSkill.Rupture.Stat"),
                    formatRuptureChance(locale, ruptureChance, lucky)));
            rows.add(MechanicRow.custom(locale.text(
                    "Swords.SubSkill.Rupture.Stat.Extra",
                    Integer.toString(settings.ruptureDurationSeconds(true)),
                    Integer.toString(settings.ruptureDurationSeconds(false)))));
            rows.add(MechanicRow.custom(locale.text(
                    "Swords.SubSkill.Rupture.Stat.TickDamage",
                    number(settings.ruptureTickDamage(ruptureRank, true)),
                    number(settings.ruptureTickDamage(ruptureRank, false)))));
            rows.add(MechanicRow.custom(locale.text("Swords.Combat.Rupture.Note.Update.One")));
        }
        if (settings.serratedRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.SWORDS_SERRATED_STRIKES, true))) {
            int baseDuration = settings.serratedDurationSeconds(level);
            int enduranceBonus = player == null ? 0 : SwordsPerks.activationBonusSeconds(
                    player.getCommandSource(), permissions);
            rows.add(new MechanicRow(
                    locale.text("Swords.SubSkill.SerratedStrikes.Stat"),
                    formatSerratedDuration(locale, baseDuration, enduranceBonus)));
        }
        if (settings.stabRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.SWORDS_STAB, true))) {
            rows.add(new MechanicRow(
                    locale.text("Swords.SubSkill.Stab.Stat"),
                    number(settings.stabDamage(level))));
        }
        if (settings.limitBreakRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.SWORDS_LIMIT_BREAK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Swords.SubSkill.SwordsLimitBreak.Stat"),
                    Integer.toString(SwordsDamage.limitBreakDamage(
                            settings.limitBreakRank(level), 1000))));
        }
        return List.copyOf(rows);
    }

    static String formatRuptureChance(
            LocaleService locale, double configuredChance, boolean lucky) {
        String value = Double.toString(configuredChance) + "%";
        return lucky
                ? value + locale.text(
                        "Perks.Lucky.Bonus", Double.toString(configuredChance * 1.33D))
                : value;
    }

    static String formatSerratedDuration(
            LocaleService locale, int baseDuration, int enduranceBonus) {
        String value = Integer.toString(baseDuration);
        return enduranceBonus > 0
                ? value + locale.text(
                        "Perks.ActivationTime.Bonus", baseDuration + enduranceBonus)
                : value;
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

    private static String number(double value) {
        return Double.toString(value);
    }
}
