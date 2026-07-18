package io.github.njw3995.fabricmmo.core.administration;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.party.PartyService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

/** Upstream-shaped periodic user purge and stale party-member cleanup scheduler. */
public final class ScheduledMaintenanceService {
    private final ProgressionMaintenanceService maintenance;
    private final PartyService parties;
    private final Clock clock;
    private final int purgeIntervalHours;
    private final int oldUserCutoffMonths;
    private final int partyKickIntervalHours;
    private final int oldPartyMemberCutoffDays;
    private final Consumer<String> logger;
    private final AtomicBoolean purgeRunning = new AtomicBoolean();
    private long nextPurgeTick;
    private long nextPartyKickTick;

    private ScheduledMaintenanceService(ProgressionMaintenanceService maintenance, PartyService parties,
            Clock clock, int purgeIntervalHours, int oldUserCutoffMonths,
            int partyKickIntervalHours, int oldPartyMemberCutoffDays, Consumer<String> logger) {
        this.maintenance = maintenance;
        this.parties = parties;
        this.clock = clock;
        this.purgeIntervalHours = purgeIntervalHours;
        this.oldUserCutoffMonths = oldUserCutoffMonths;
        this.partyKickIntervalHours = partyKickIntervalHours;
        this.oldPartyMemberCutoffDays = oldPartyMemberCutoffDays;
        this.logger = logger;
        nextPurgeTick = initialTick(purgeIntervalHours);
        nextPartyKickTick = initialTick(partyKickIntervalHours);
    }

    public static ScheduledMaintenanceService load(Path configFile,
            ProgressionMaintenanceService maintenance, PartyService parties,
            Clock clock, Consumer<String> logger) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        return new ScheduledMaintenanceService(maintenance, parties, clock,
                config.integer("Database_Purging.Purge_Interval", -1),
                config.integer("Database_Purging.Old_User_Cutoff", 6),
                config.integer("Party.AutoKick_Interval", 12),
                config.integer("Party.Old_Party_Member_Cutoff", 7), logger);
    }

    public void tick(MinecraftServer server) {
        long tick = server.getTicks();
        if (nextPurgeTick >= 0 && tick >= nextPurgeTick) {
            nextPurgeTick = recurringTick(tick, purgeIntervalHours);
            runPurge();
        }
        if (nextPartyKickTick >= 0 && tick >= nextPartyKickTick) {
            nextPartyKickTick = recurringTick(tick, partyKickIntervalHours);
            runPartyCleanup(server);
        }
    }

    private void runPurge() {
        if (!purgeRunning.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                int powerless = maintenance.purgePowerless().removedPlayers();
                int old = oldUserCutoffMonths == -1 ? 0
                        : maintenance.purgeOldUsers(oldUserCutoffMonths).removedPlayers();
                logger.accept("Scheduled purge removed " + powerless + " powerless and " + old + " old users");
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }).whenComplete((ignored, failure) -> {
            purgeRunning.set(false);
            if (failure != null) logger.accept("Scheduled purge failed: " + failure.getMessage());
        });
    }

    private void runPartyCleanup(MinecraftServer server) {
        Set<UUID> online = server.getPlayerManager().getPlayerList().stream()
                .map(player -> player.getUuid()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Instant cutoff = clock.instant().minus(Math.max(0, oldPartyMemberCutoffDays), ChronoUnit.DAYS);
        var result = parties.removeInactiveMembers(online, id -> {
            try { return maintenance.activeStore().lastSeen(id); }
            catch (IOException exception) { return Instant.EPOCH; }
        }, cutoff);
        logger.accept("Scheduled party cleanup removed " + result.removedMembers() + " stale members");
    }

    private static long initialTick(int hours) {
        if (hours < 0) return -1L;
        return hours == 0 ? 40L : hours * 60L * 60L * 20L;
    }

    private static long recurringTick(long now, int hours) {
        return hours <= 0 ? -1L : now + hours * 60L * 60L * 20L;
    }
}
