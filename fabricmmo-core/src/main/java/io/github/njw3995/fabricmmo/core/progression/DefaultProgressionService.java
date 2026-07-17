package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.event.XpAwardedEvent;
import io.github.njw3995.fabricmmo.api.event.XpPreAwardEvent;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistryView;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.ProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.StoredSkillProgress;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** mcMMO-shaped skill progression, including configured caps and XP modifiers. */
public final class DefaultProgressionService implements ProgressionService {
    private final SkillRegistryView registry;
    private final ProgressionStore store;
    private final ProgressionFormula formula;
    private final XpSourceRegistryView xpSources;
    private final FabricMmoEventBus eventBus;
    private final ProgressionSettings settings;
    private final DiminishedReturnsTracker diminishedReturns;
    private final ConcurrentHashMap<UUID, Object> playerLocks = new ConcurrentHashMap<>();

    public DefaultProgressionService(
            SkillRegistryView registry,
            ProgressionStore store,
            ProgressionFormula formula,
            XpSourceRegistryView xpSources,
            FabricMmoEventBus eventBus,
            ProgressionSettings settings,
            Clock clock) {
        this.registry = registry;
        this.store = store;
        this.formula = formula;
        this.xpSources = xpSources;
        this.eventBus = eventBus;
        this.settings = settings;
        this.diminishedReturns = new DiminishedReturnsTracker(clock);
    }

    @Override
    public ProgressionSnapshot query(UUID playerId, NamespacedId skillId) {
        requireRegisteredSkill(skillId);
        PlayerProgressionData data = load(playerId);
        StoredSkillProgress progress = data.skills()
                .getOrDefault(skillId, new StoredSkillProgress(0, 0.0D));
        return snapshot(playerId, skillId, progress, data);
    }

    @Override
    public XpAwardResult award(XpAwardRequest request) {
        var skill = registry.find(request.skillId());
        if (skill.isEmpty()) {
            return rejected("Unknown skill " + request.skillId(), 0);
        }
        if (skill.orElseThrow().childSkill()) {
            return rejected("Child skills do not receive direct XP", 0);
        }
        var source = xpSources.find(request.sourceId());
        if (source.isEmpty()) {
            return rejected("Unknown XP source " + request.sourceId(), 0);
        }
        XpSourceDefinition sourceDefinition = source.orElseThrow();
        if (!sourceDefinition.skillId().equals(request.skillId())) {
            return rejected("XP source " + request.sourceId() + " does not target "
                    + request.skillId(), 0);
        }

        synchronized (playerLocks.computeIfAbsent(request.playerId(), ignored -> new Object())) {
            PlayerProgressionData existing = load(request.playerId());
            TreeMap<NamespacedId, StoredSkillProgress> updated = new TreeMap<>(existing.skills());
            StoredSkillProgress current = updated.getOrDefault(
                    request.skillId(), new StoredSkillProgress(0, 0.0D));
            Set<NamespacedId> permittedPowerSkills = permittedPowerLevelSkills(request);
            int powerLevelUpperBound = powerLevel(existing, null);
            int permissionAwarePowerLevel = powerLevel(existing, permittedPowerSkills);
            int powerLevelForCurve = permissionAwarePowerLevel;
            boolean countsTowardPermissionAwarePower = permittedPowerSkills == null
                    || permittedPowerSkills.contains(request.skillId());
            int skillCap = settings.levelCap(request.skillId());
            if (current.level() >= skillCap
                    || hasReachedPowerLevelCap(
                            powerLevelUpperBound, permissionAwarePowerLevel)) {
                return rejected("Level cap reached", current.level());
            }

            boolean command = commandSource(sourceDefinition);
            float modifiedXp = (float) request.rawXp();
            double effectiveGlobalMultiplier = effectiveXpRateMultiplier(request);
            if (!command) {
                modifiedXp = (float) (modifiedXp
                        * settings.skillXpMultiplier(request.skillId())
                        * effectiveGlobalMultiplier);
                modifiedXp = (float) (modifiedXp
                        * contextMultiplier(request, "xpPerkMultiplier"));
            }

            XpAwardRequest modifiedRequest = requestWithXp(request, modifiedXp);
            XpPreAwardEvent preEvent = eventBus.publish(new XpPreAwardEvent(modifiedRequest));
            if (preEvent.cancelled() || preEvent.multiplier() == 0.0D) {
                return new XpAwardResult(
                        XpAwardResult.Status.CANCELLED, 0, current.level(), current.level(),
                        "XP award cancelled");
            }
            float appliedXp = (float) (modifiedXp * preEvent.multiplier());
            if (!command) {
                if (settings.earlyGameBoostEnabled() && current.level() < 1) {
                    appliedXp += (int) (xpToNextLevel(
                            current.level(), powerLevelForCurve) * 0.05D);
                }
                int threshold = settings.diminishedReturnsThreshold(request.skillId());
                if (settings.diminishedReturnsEnabled() && threshold > 0 && appliedXp > 0.0D) {
                    float registered = diminishedReturns.registeredXp(
                            request.playerId(), request.skillId(),
                            settings.diminishedReturnsInterval());
                    DiminishedReturns.Result result = DiminishedReturns.apply(
                            appliedXp,
                            registered,
                            threshold,
                            settings.skillXpMultiplier(request.skillId()),
                            effectiveGlobalMultiplier,
                            (float) settings.diminishedReturnsMinimumFraction());
                    if (result.cancelled()) {
                        return new XpAwardResult(
                                XpAwardResult.Status.CANCELLED,
                                0,
                                current.level(),
                                current.level(),
                                "XP cancelled by diminished returns");
                    }
                    appliedXp = result.xp();
                }
            }
            if (!Double.isFinite(appliedXp) || appliedXp <= 0.0D) {
                return rejected("XP modified to zero", current.level());
            }

            int oldLevel = current.level();
            int level = current.level();
            float xp = (float) current.xp() + appliedXp;
            int levelsGained = 0;
            while (true) {
                int currentPowerForCurve = powerAfterLevelGains(
                        powerLevelForCurve, levelsGained, countsTowardPermissionAwarePower);
                int requiredXp = xpToNextLevel(level, currentPowerForCurve);
                if (xp < requiredXp) {
                    break;
                }
                int currentPowerUpperBound = powerAfterLevelGains(
                        powerLevelUpperBound, levelsGained, true);
                int currentPermissionAwarePower = powerAfterLevelGains(
                        permissionAwarePowerLevel,
                        levelsGained,
                        countsTowardPermissionAwarePower);
                if (level >= skillCap || hasReachedPowerLevelCap(
                        currentPowerUpperBound, currentPermissionAwarePower)) {
                    xp = 0.0F;
                    break;
                }
                xp -= requiredXp;
                level++;
                levelsGained++;
            }

            updated.put(request.skillId(), new StoredSkillProgress(level, xp));
            save(new PlayerProgressionData(request.playerId(), existing.revision() + 1, updated));
            if (!command) {
                diminishedReturns.register(
                        request.playerId(), request.skillId(), appliedXp,
                        settings.diminishedReturnsInterval());
            }
            int reportedXp = visibleInt(appliedXp);
            XpAwardResult result = new XpAwardResult(
                    XpAwardResult.Status.APPLIED, reportedXp, oldLevel, level, "");
            XpAwardRequest appliedRequest = requestWithXp(request, appliedXp);
            eventBus.publish(new XpAwardedEvent(appliedRequest, result));
            if (level != oldLevel) {
                eventBus.publish(new LevelChangedEvent(
                        request.playerId(), request.skillId(), oldLevel, level));
            }
            return result;
        }
    }

    @Override
    public Map<NamespacedId, ProgressionSnapshot> queryAll(UUID playerId) {
        PlayerProgressionData data = load(playerId);
        Map<NamespacedId, ProgressionSnapshot> snapshots = new TreeMap<>();
        for (var skill : registry.skills()) {
            StoredSkillProgress progress = data.skills().getOrDefault(
                    skill.id(), new StoredSkillProgress(0, 0.0D));
            snapshots.put(skill.id(), snapshot(playerId, skill.id(), progress, data));
        }
        return Map.copyOf(snapshots);
    }

    private ProgressionSnapshot snapshot(
            UUID playerId,
            NamespacedId skillId,
            StoredSkillProgress progress,
            PlayerProgressionData data) {
        int toNext = xpToNextLevel(progress.level(), powerLevel(data, null));
        return new ProgressionSnapshot(
                playerId, skillId, progress.level(), visibleInt(progress.xp()), toNext);
    }

    private int xpToNextLevel(int skillLevel, int powerLevel) {
        int curveLevel = settings.cumulativeCurve() ? powerLevel : skillLevel;
        return formula.xpToNextLevel(curveLevel, settings.mode(), settings.formulaType());
    }

    private boolean hasReachedPowerLevelCap(
            int powerLevelUpperBound,
            int permissionAwarePowerLevel) {
        return powerLevelUpperBound >= settings.powerLevelCap()
                && permissionAwarePowerLevel >= settings.powerLevelCap();
    }

    private static int powerAfterLevelGains(
            int basePowerLevel,
            int levelsGained,
            boolean countsTowardPower) {
        if (!countsTowardPower) {
            return basePowerLevel;
        }
        long power = (long) basePowerLevel + levelsGained;
        return power >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) power;
    }

    private int powerLevel(PlayerProgressionData data, Set<NamespacedId> permittedSkills) {
        long power = 0L;
        for (var skill : registry.skills()) {
            if (skill.childSkill()
                    || (permittedSkills != null && !permittedSkills.contains(skill.id()))) {
                continue;
            }
            power += data.skills().getOrDefault(
                    skill.id(), new StoredSkillProgress(0, 0.0D)).level();
            if (power >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) power;
    }

    private static Set<NamespacedId> permittedPowerLevelSkills(XpAwardRequest request) {
        String value = request.context().get("powerLevelSkills");
        if (value == null) {
            return null;
        }
        Set<NamespacedId> result = new HashSet<>();
        if (value.isBlank()) {
            return result;
        }
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(NamespacedId.parse(trimmed));
            } catch (IllegalArgumentException ignored) {
                // An invalid context entry cannot grant permission or inflate power level.
            }
        }
        return result;
    }

    private static boolean commandSource(XpSourceDefinition source) {
        return "XPGainReason.COMMAND".equals(source.metadata().get("upstreamReason"));
    }

    private double effectiveXpRateMultiplier(XpAwardRequest request) {
        String configured = request.context().get("serverXpRateMultiplier");
        if (configured == null) {
            return settings.globalXpMultiplier();
        }
        try {
            double override = Double.parseDouble(configured);
            if (!Double.isFinite(override) || override <= 0.0D) {
                return settings.globalXpMultiplier();
            }
            // mcMMO's per-skill /xprate override does not stack with the global rate.
            return Math.max(override, settings.globalXpMultiplier());
        } catch (NumberFormatException ignored) {
            return settings.globalXpMultiplier();
        }
    }

    private static double contextMultiplier(XpAwardRequest request, String key) {
        String configured = request.context().get(key);
        if (configured == null) {
            return 1.0D;
        }
        try {
            double multiplier = Double.parseDouble(configured);
            return Double.isFinite(multiplier) && multiplier > 0.0D ? multiplier : 1.0D;
        } catch (NumberFormatException ignored) {
            return 1.0D;
        }
    }

    private static XpAwardRequest requestWithXp(XpAwardRequest request, float xp) {
        return new XpAwardRequest(
                request.playerId(), request.skillId(), request.sourceId(), xp, request.context());
    }

    private static int visibleInt(double value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.floor(value);
    }

    private static XpAwardResult rejected(String detail, int level) {
        return new XpAwardResult(XpAwardResult.Status.REJECTED, 0, level, level, detail);
    }

    private PlayerProgressionData load(UUID playerId) {
        try {
            PlayerProgressionData loaded = store.load(playerId);
            PlayerProgressionData normalized = truncateAboveConfiguredCaps(loaded);
            if (normalized != loaded) {
                store.save(normalized);
            }
            return normalized;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private PlayerProgressionData truncateAboveConfiguredCaps(PlayerProgressionData data) {
        if (!settings.truncateSkills()) {
            return data;
        }
        TreeMap<NamespacedId, StoredSkillProgress> truncated = null;
        for (Map.Entry<NamespacedId, StoredSkillProgress> entry : data.skills().entrySet()) {
            int cap = settings.levelCap(entry.getKey());
            if (cap == Integer.MAX_VALUE || entry.getValue().level() <= cap) {
                continue;
            }
            if (truncated == null) {
                truncated = new TreeMap<>(data.skills());
            }
            truncated.put(entry.getKey(), new StoredSkillProgress(cap, 0.0D));
        }
        if (truncated == null) {
            return data;
        }
        return new PlayerProgressionData(data.playerId(), data.revision() + 1, truncated);
    }

    private void save(PlayerProgressionData data) {
        try {
            store.save(data);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void requireRegisteredSkill(NamespacedId skillId) {
        if (registry.find(skillId).isEmpty()) {
            throw new IllegalArgumentException("Unknown skill: " + skillId);
        }
    }
}
