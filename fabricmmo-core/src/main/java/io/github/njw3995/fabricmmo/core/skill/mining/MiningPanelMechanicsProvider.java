package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Mining mechanic rows rendered through the same shared skill panel as every other skill. */
public final class MiningPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final MiningSettings settings;
    private final MiningDropSettings drops;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public MiningPanelMechanicsProvider(
            MinecraftServer server,
            MiningSettings settings,
            MiningDropSettings drops) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.drops = Objects.requireNonNull(drops, "drops");
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

        if (!inspectPermissions || allowed(player, PermissionNodes.MINING_SUPER_BREAKER, true)) {
            int duration = settings.superBreakerDurationSeconds(level) + activationBonus;
            String value;
            if (level < settings.superBreakerUnlockLevel()) {
                value = "Locked until level " + settings.superBreakerUnlockLevel();
            } else if (isSuperBreakerActive(playerId)) {
                value = "ACTIVE (" + superBreakerSecondsRemaining(playerId) + "s remaining)";
            } else {
                int cooldown = superBreakerCooldownRemaining(
                        playerId,
                        inspectPermissions
                                ? MiningPerks.cooldownSeconds(settings.superBreakerCooldownSeconds(),
                                        player.getCommandSource(), permissions)
                                : settings.superBreakerCooldownSeconds());
                value = duration + " seconds" + (cooldown > 0 ? ", cooldown " + cooldown + "s" : ", ready");
            }
            rows.add(new MechanicRow("Super Breaker", value));
        }

        if (!inspectPermissions || allowed(player, PermissionNodes.MINING_DOUBLE_DROPS, true)) {
            rows.add(new MechanicRow("Double Drops",
                    drops.doubleDropsUnlocked(level, settings.progressionMode())
                            ? chance(level, drops.doubleDropsMaxLevel(settings.progressionMode()),
                                    drops.doubleDropsChanceMaxPercent(), lucky)
                            : "Locked"));
        }
        if (!inspectPermissions || allowed(player, PermissionNodes.MINING_MOTHER_LODE, true)) {
            rows.add(new MechanicRow("Mother Lode",
                    drops.motherLodeUnlocked(level, settings.progressionMode())
                            ? chance(level, drops.motherLodeMaxLevel(settings.progressionMode()),
                                    drops.motherLodeChanceMaxPercent(), lucky)
                            : "Locked"));
        }

        int rank = settings.blastRank(level);
        if (!inspectPermissions || allowed(player, PermissionNodes.MINING_BLAST_MINING, true)) {
            String value = rank == 0
                    ? "Locked"
                    : "Rank " + rank + '/' + MiningSettings.BLAST_RANKS
                            + " (Ore Bonus " + decimal(settings.oreBonusFraction(rank) * 100.0D)
                            + "%, Drop Multiplier " + settings.dropMultiplier(rank) + "x)";
            rows.add(new MechanicRow("Blast Mining", value));
        }
        if (!inspectPermissions || allowed(player, PermissionNodes.MINING_BIGGER_BOMBS, true)) {
            rows.add(new MechanicRow("Bigger Bombs",
                    level >= settings.biggerBombsUnlockLevel()
                            ? "Radius Increase: +" + decimal(settings.blastRadiusModifier(rank))
                            : "Locked until level " + settings.biggerBombsUnlockLevel()));
        }
        if (!inspectPermissions || allowed(player, PermissionNodes.MINING_DEMOLITIONS_EXPERTISE, true)) {
            rows.add(new MechanicRow("Demolitions Expertise",
                    level >= settings.demolitionsExpertiseUnlockLevel()
                            ? "Damage Decrease: " + decimal(settings.blastDamageDecreasePercent(rank)) + '%'
                            : "Locked until level " + settings.demolitionsExpertiseUnlockLevel()));
        }
        return List.copyOf(rows);
    }

    private static boolean isSuperBreakerActive(UUID playerId) {
        try {
            return FabricMmoFabricRuntime.miningAbilities().isSuperBreakerActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Super Breaker active state", exception);
        }
    }

    private static int superBreakerSecondsRemaining(UUID playerId) {
        try {
            return FabricMmoFabricRuntime.miningAbilities().superBreakerSecondsRemaining(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Super Breaker duration", exception);
        }
    }

    private static int superBreakerCooldownRemaining(UUID playerId, int cooldownSeconds) {
        try {
            return FabricMmoFabricRuntime.miningAbilities()
                    .superBreakerCooldownRemaining(playerId, cooldownSeconds);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Super Breaker cooldown", exception);
        }
    }

    private boolean allowed(ServerPlayerEntity player, String permission, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static String chance(int level, int maxLevel, double maximum, boolean lucky) {
        double base = MiningProbability.chance(level, maxLevel, maximum, false) * 100.0D;
        if (!lucky) {
            return decimal(base) + '%';
        }
        double boosted = MiningProbability.chance(level, maxLevel, maximum, true) * 100.0D;
        return decimal(base) + "% (" + decimal(boosted) + "% with Lucky Perk)";
    }

    private static String decimal(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
