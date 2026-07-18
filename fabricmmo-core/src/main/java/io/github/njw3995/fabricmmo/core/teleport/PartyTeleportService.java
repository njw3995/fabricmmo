package io.github.njw3995.fabricmmo.core.teleport;

import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.party.PartyFeature;
import io.github.njw3995.fabricmmo.core.party.PartyService;
import io.github.njw3995.fabricmmo.core.party.PartySettings;
import io.github.njw3995.fabricmmo.core.party.PartyState;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.player.PlayerVisibilityService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/** Full party-teleport request, warmup, cooldown, combat, world, visibility, and protection service. */
public final class PartyTeleportService {
    private final MinecraftServer server;
    private final PartyService parties;
    private final PartySettings settings;
    private final CommandPermissionService permissions;
    private final ProtectionService protection;
    private final PlayerVisibilityService visibility;
    private final Clock clock;
    private final Map<UUID, TeleportPreferences> preferences = new HashMap<>();
    private final Map<UUID, Map<UUID, Instant>> requests = new HashMap<>();
    private final Map<UUID, Instant> lastUse = new HashMap<>();
    private final Map<UUID, Instant> recentlyHurt = new HashMap<>();
    private final Map<UUID, Warmup> warmups = new HashMap<>();

    public PartyTeleportService(
            MinecraftServer server,
            PartyService parties,
            CommandPermissionService permissions,
            ProtectionService protection,
            PlayerVisibilityService visibility,
            Clock clock) {
        this.server = Objects.requireNonNull(server, "server");
        this.parties = Objects.requireNonNull(parties, "parties");
        this.settings = parties.settings();
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.protection = Objects.requireNonNull(protection, "protection");
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized Result request(ServerPlayerEntity requester, ServerPlayerEntity target) {
        evictExpiredRequests();
        Result validation = validatePair(requester, target, true);
        if (!validation.success()) {
            return validation;
        }
        int cooldown = remaining(lastUse.get(requester.getUuid()), settings.ptpCooldown());
        if (cooldown > 0) {
            return Result.fail("You must wait " + cooldown + " seconds before teleporting again.");
        }
        int hurt = hurtRemaining(requester.getUuid());
        if (hurt > 0) {
            return Result.fail("You were recently hurt. Wait " + hurt + " seconds.");
        }
        if (!worldAllowed(requester, target)) {
            return Result.fail("You do not have permission to teleport to world " + worldName(target) + ".");
        }
        if (!preferences(target.getUuid()).confirmRequired()) {
            return beginWarmup(requester, target);
        }
        requests.computeIfAbsent(target.getUuid(), ignored -> new HashMap<>())
                .put(requester.getUuid(), clock.instant().plus(settings.ptpRequestTimeout()));
        return Result.ok("Teleport request sent.");
    }

    public synchronized Result accept(ServerPlayerEntity target, Optional<UUID> requesterId) {
        evictExpiredRequests();
        int hurt = hurtRemaining(target.getUuid());
        if (hurt > 0) {
            return Result.fail("You were recently hurt. Wait " + hurt + " seconds.");
        }
        Map<UUID, Instant> pending = requests.get(target.getUuid());
        if (pending == null || pending.isEmpty()) {
            return Result.fail("No pending party teleport requests.");
        }
        UUID selected = requesterId.filter(pending::containsKey).orElseGet(() -> pending.entrySet()
                .stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null));
        if (selected == null) {
            return Result.fail("No matching party teleport request.");
        }
        pending.remove(selected);
        if (pending.isEmpty()) {
            requests.remove(target.getUuid());
        }
        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(selected);
        if (requester == null) {
            return Result.fail("The requesting player is no longer online.");
        }
        Result validation = validatePair(requester, target, true);
        if (!validation.success()) {
            return validation;
        }
        if (!worldAllowed(requester, target)) {
            return Result.fail("The requester cannot teleport to world " + worldName(target) + ".");
        }
        return beginWarmup(requester, target);
    }

    public synchronized TeleportPreferences toggleEnabled(UUID playerId) {
        TeleportPreferences current = preferences(playerId);
        TeleportPreferences next = new TeleportPreferences(!current.enabled(), current.confirmRequired());
        preferences.put(playerId, next);
        return next;
    }

    public synchronized TeleportPreferences toggleAcceptAny(UUID playerId) {
        TeleportPreferences current = preferences(playerId);
        TeleportPreferences next = new TeleportPreferences(current.enabled(), !current.confirmRequired());
        preferences.put(playerId, next);
        return next;
    }

    public synchronized TeleportPreferences preferences(UUID playerId) {
        return preferences.getOrDefault(
                playerId, new TeleportPreferences(true, settings.ptpAcceptRequired()));
    }

    public synchronized void markHurt(UUID playerId) {
        recentlyHurt.put(playerId, clock.instant());
        Warmup warmup = warmups.remove(playerId);
        if (warmup != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                player.sendMessage(Text.literal("Teleport cancelled because you were hurt.")
                        .formatted(Formatting.RED));
            }
        }
    }

    public synchronized void tick() {
        evictExpiredRequests();
        Instant now = clock.instant();
        Iterator<Map.Entry<UUID, Warmup>> iterator = warmups.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Warmup> entry = iterator.next();
            UUID requesterId = entry.getKey();
            Warmup warmup = entry.getValue();
            ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterId);
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(warmup.targetId());
            if (requester == null || target == null) {
                iterator.remove();
                continue;
            }
            if (requester.getPos().squaredDistanceTo(warmup.start()) > 1.0D) {
                requester.sendMessage(Text.literal("Teleport cancelled because you moved.")
                        .formatted(Formatting.RED));
                iterator.remove();
                continue;
            }
            if (!now.isBefore(warmup.executeAt())) {
                iterator.remove();
                Result validation = validatePair(requester, target, false);
                if (!validation.success() || hurtRemaining(requesterId) > 0
                        || !worldAllowed(requester, target)) {
                    requester.sendMessage(Text.literal(validation.success()
                            ? "Teleport cancelled because its restrictions changed."
                            : validation.detail()).formatted(Formatting.RED));
                    continue;
                }
                if (!protectionAllows(requester, target)) {
                    requester.sendMessage(Text.literal("Teleport denied by area protection.")
                            .formatted(Formatting.RED));
                    continue;
                }
                requester.teleport(
                        target.getServerWorld(), target.getX(), target.getY(), target.getZ(),
                        Set.of(), target.getYaw(), target.getPitch());
                lastUse.put(requesterId, now);
                requester.sendMessage(Text.literal("Party teleport complete.").formatted(Formatting.GREEN));
            }
        }
    }

    public synchronized void remove(UUID playerId) {
        preferences.remove(playerId);
        requests.remove(playerId);
        requests.values().forEach(map -> map.remove(playerId));
        lastUse.remove(playerId);
        recentlyHurt.remove(playerId);
        warmups.remove(playerId);
    }

    public synchronized void clear() {
        preferences.clear();
        requests.clear();
        lastUse.clear();
        recentlyHurt.clear();
        warmups.clear();
    }

    private Result beginWarmup(ServerPlayerEntity requester, ServerPlayerEntity target) {
        if (!protectionAllows(requester, target)) {
            return Result.fail("Teleport denied by area protection.");
        }
        Duration warmup = settings.ptpWarmup();
        if (warmup.isZero()) {
            requester.teleport(
                    target.getServerWorld(), target.getX(), target.getY(), target.getZ(),
                    Set.of(), target.getYaw(), target.getPitch());
            lastUse.put(requester.getUuid(), clock.instant());
            return Result.ok("Party teleport complete.");
        }
        warmups.put(requester.getUuid(), new Warmup(
                target.getUuid(), requester.getPos(), clock.instant().plus(warmup)));
        return Result.ok("Teleport commencing in " + warmup.toSeconds() + " seconds. Do not move.");
    }

    private Result validatePair(
            ServerPlayerEntity requester,
            ServerPlayerEntity target,
            boolean checkTargetPreference) {
        if (requester.equals(target)) {
            return Result.fail("You cannot teleport to yourself.");
        }
        PartyState party = parties.partyOf(requester.getUuid()).orElse(null);
        if (party == null) {
            return Result.fail("You are not in a party.");
        }
        if (!parties.featureUnlocked(party, PartyFeature.TELEPORT)) {
            return Result.fail("Party teleport unlocks at party level "
                    + settings.unlockLevel(PartyFeature.TELEPORT) + ".");
        }
        if (!party.members().contains(target.getUuid())) {
            return Result.fail(target.getGameProfile().getName() + " is not in your party.");
        }
        if (checkTargetPreference && !preferences(target.getUuid()).enabled()) {
            return Result.fail(target.getGameProfile().getName() + " has party teleport disabled.");
        }
        if (!target.isAlive() || target.isRemoved()) {
            return Result.fail("The target cannot be teleported to right now.");
        }
        if (!visibility.visibleTo(target, requester)) {
            return Result.fail("The target player is hidden.");
        }
        if (FabricMmoFabricRuntime.isWorldBlacklisted(requester.getServerWorld())
                || FabricMmoFabricRuntime.isWorldBlacklisted(target.getServerWorld())) {
            return Result.fail("Party teleport is disabled in this world.");
        }
        return Result.ok("");
    }

    private boolean worldAllowed(ServerPlayerEntity requester, ServerPlayerEntity target) {
        if (!settings.ptpWorldPermissions()) {
            return true;
        }
        if (permissions.hasPermission(
                requester.getCommandSource(), "mcmmo.commands.ptp.world.all", false)) {
            return true;
        }
        String targetPermission = "mcmmo.commands.ptp.world." + worldName(target);
        // Upstream requires the destination player to be permitted in the destination world.
        if (!permissions.hasPermission(target.getCommandSource(), targetPermission, false)) {
            return false;
        }
        // Cross-world teleports additionally require the teleporting player to enter that world.
        return requester.getServerWorld() == target.getServerWorld()
                || permissions.hasPermission(requester.getCommandSource(), targetPermission, false);
    }

    private boolean protectionAllows(ServerPlayerEntity requester, ServerPlayerEntity target) {
        String worldId = target.getServerWorld().getRegistryKey().getValue().toString();
        return protection.canInteract(
                requester.getUuid(), worldId,
                target.getBlockX(), target.getBlockY(), target.getBlockZ());
    }

    private int hurtRemaining(UUID playerId) {
        return remaining(recentlyHurt.get(playerId), settings.ptpRecentlyHurtCooldown());
    }

    private int remaining(Instant start, Duration duration) {
        if (start == null || duration.isZero()) {
            return 0;
        }
        Instant available = start.plus(duration);
        Duration remaining = Duration.between(clock.instant(), available);
        if (remaining.isNegative() || remaining.isZero()) {
            return 0;
        }
        return (int) Math.max(1L, remaining.toSeconds() + (remaining.toNanosPart() > 0 ? 1L : 0L));
    }

    private void evictExpiredRequests() {
        Instant now = clock.instant();
        requests.values().forEach(map -> map.entrySet().removeIf(entry -> !entry.getValue().isAfter(now)));
        requests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private static String worldName(ServerPlayerEntity player) {
        return player.getServerWorld().getRegistryKey().getValue().getPath();
    }

    public record TeleportPreferences(boolean enabled, boolean confirmRequired) {
    }

    public record Result(boolean success, String detail) {
        public static Result ok(String detail) {
            return new Result(true, detail);
        }

        public static Result fail(String detail) {
            return new Result(false, detail);
        }
    }

    private record Warmup(UUID targetId, Vec3d start, Instant executeAt) {
    }
}
