package io.github.njw3995.fabricmmo.core.skill.mining;

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

/** Exact mcMMO 2.3.000 Mining statsDisplay rows. */
public final class MiningPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final MiningSettings settings;
    private final MiningDropSettings drops;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public MiningPanelMechanicsProvider(
            MinecraftServer server,
            MiningSettings settings,
            MiningDropSettings drops,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.drops = Objects.requireNonNull(drops, "drops");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspectPermissions = player != null;
        boolean lucky = inspectPermissions && allowed(player, PermissionNodes.MINING_LUCKY, false);
        int activationBonus = inspectPermissions
                ? MiningPerks.activationBonusSeconds(player.getCommandSource(), permissions)
                : 0;
        ArrayList<MechanicRow> rows = new ArrayList<>();
        int rank = settings.blastRank(level);

        // Match upstream MiningCommand.statsDisplay ordering and templates exactly.
        if (level >= settings.biggerBombsUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.MINING_BIGGER_BOMBS, true))) {
            rows.add(MechanicRow.custom(locale.text(
                    "Mining.SubSkill.BlastMining.Stat.Extra",
                    Double.toString(settings.blastRadiusModifier(rank)))));
        }
        if (rank > 0
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.MINING_BLAST_MINING, true))) {
            String effect = locale.text(
                    "Mining.Blast.Effect",
                    formatProbability(settings.oreBonusFraction(rank)),
                    settings.dropMultiplier(rank));
            rows.add(MechanicRow.custom(locale.text(
                    "Mining.SubSkill.BlastMining.Stat",
                    rank,
                    MiningSettings.BLAST_RANKS,
                    effect)));
        }
        if (level >= settings.demolitionsExpertiseUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.MINING_DEMOLITIONS_EXPERTISE, true))) {
            rows.add(new MechanicRow(
                    locale.text("Mining.SubSkill.DemolitionsExpertise.Stat"),
                    formatPercentage(settings.blastDamageDecreasePercent(rank))));
        }
        if (drops.doubleDropsUnlocked(level, settings.progressionMode())
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.MINING_DOUBLE_DROPS, true))) {
            rows.add(new MechanicRow(
                    locale.text("Mining.SubSkill.DoubleDrops.Stat"),
                    chance(
                            level,
                            drops.doubleDropsMaxLevel(settings.progressionMode()),
                            drops.doubleDropsChanceMaxPercent(),
                            lucky)));
        }
        if (drops.motherLodeUnlocked(level, settings.progressionMode())
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.MINING_MOTHER_LODE, true))) {
            rows.add(new MechanicRow(
                    locale.text("Mining.SubSkill.MotherLode.Stat"),
                    chance(
                            level,
                            drops.motherLodeMaxLevel(settings.progressionMode()),
                            drops.motherLodeChanceMaxPercent(),
                            lucky)));
        }
        if (level >= settings.superBreakerUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.MINING_SUPER_BREAKER, true))) {
            int baseDuration = settings.superBreakerDurationSeconds(level);
            String value = Integer.toString(baseDuration);
            if (activationBonus > 0) {
                value += locale.text("Perks.ActivationTime.Bonus", baseDuration + activationBonus);
            }
            rows.add(new MechanicRow(
                    locale.text("Mining.SubSkill.SuperBreaker.Stat"),
                    value));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String permission, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private String chance(int level, int maxLevel, double maximum, boolean lucky) {
        double base = MiningProbability.chance(level, maxLevel, maximum, false);
        String value = formatProbability(base);
        if (lucky) {
            double boosted = MiningProbability.chance(level, maxLevel, maximum, true);
            value += locale.text("Perks.Lucky.Bonus", formatProbability(boosted));
        }
        return value;
    }

    private static String formatProbability(double probability) {
        return String.format(Locale.US, "%.2f%%", probability * 100.0D);
    }

    private static String formatPercentage(double percentage) {
        return String.format(Locale.US, "%.2f%%", percentage);
    }
}
