package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.event.XpAwardedEvent;
import io.github.njw3995.fabricmmo.api.event.XpPreAwardEvent;
import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistryView;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.ProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.StoredSkillProgress;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultProgressionService implements ProgressionService {
    private final SkillRegistryView registry;
    private final ProgressionStore store;
    private final ProgressionFormula formula;
    private final XpSourceRegistryView xpSources;
    private final FabricMmoEventBus eventBus;
    private final ProgressionMode mode;
    private final FormulaType formulaType;
    private final ConcurrentHashMap<UUID, Object> playerLocks = new ConcurrentHashMap<>();

    public DefaultProgressionService(SkillRegistryView registry, ProgressionStore store,
                                     ProgressionFormula formula, XpSourceRegistryView xpSources,
                                     FabricMmoEventBus eventBus,
                                     ProgressionMode mode, FormulaType formulaType) {
        this.registry = registry;
        this.store = store;
        this.formula = formula;
        this.xpSources = xpSources;
        this.eventBus = eventBus;
        this.mode = mode;
        this.formulaType = formulaType;
    }

    @Override
    public ProgressionSnapshot query(UUID playerId, NamespacedId skillId) {
        requireRegisteredSkill(skillId);
        StoredSkillProgress progress = load(playerId).skills()
                .getOrDefault(skillId, new StoredSkillProgress(0, 0));
        return snapshot(playerId, skillId, progress);
    }

    @Override
    public XpAwardResult award(XpAwardRequest request) {
        if (registry.find(request.skillId()).isEmpty()) {
            return new XpAwardResult(XpAwardResult.Status.REJECTED, 0, 0, 0,
                    "Unknown skill " + request.skillId());
        }
        if (registry.find(request.skillId()).orElseThrow().childSkill()) {
            return new XpAwardResult(XpAwardResult.Status.REJECTED, 0, 0, 0,
                    "Child skills do not receive direct XP");
        }
        var source = xpSources.find(request.sourceId());
        if (source.isEmpty()) {
            return new XpAwardResult(XpAwardResult.Status.REJECTED, 0, 0, 0,
                    "Unknown XP source " + request.sourceId());
        }
        if (!source.orElseThrow().skillId().equals(request.skillId())) {
            return new XpAwardResult(XpAwardResult.Status.REJECTED, 0, 0, 0,
                    "XP source " + request.sourceId() + " does not target " + request.skillId());
        }
        XpPreAwardEvent preEvent = eventBus.publish(new XpPreAwardEvent(request));
        if (preEvent.cancelled() || preEvent.multiplier() == 0.0) {
            return new XpAwardResult(XpAwardResult.Status.CANCELLED, 0, 0, 0,
                    "XP award cancelled");
        }
        int appliedXp = (int) Math.floor(request.rawXp() * preEvent.multiplier());
        if (appliedXp <= 0) {
            return new XpAwardResult(XpAwardResult.Status.REJECTED, 0, 0, 0,
                    "XP rounded to zero");
        }

        synchronized (playerLocks.computeIfAbsent(request.playerId(), ignored -> new Object())) {
            PlayerProgressionData existing = load(request.playerId());
            TreeMap<NamespacedId, StoredSkillProgress> updated = new TreeMap<>(existing.skills());
            StoredSkillProgress current = updated.getOrDefault(request.skillId(),
                    new StoredSkillProgress(0, 0));
            int oldLevel = current.level();
            int level = current.level();
            long xp = (long) current.xp() + appliedXp;
            int cap = registry.find(request.skillId()).orElseThrow().levelCap();
            while (level < cap) {
                int required = formula.xpToNextLevel(level, mode, formulaType);
                if (xp < required) {
                    break;
                }
                xp -= required;
                level++;
            }
            if (level >= cap) {
                xp = 0;
            }
            updated.put(request.skillId(), new StoredSkillProgress(level, Math.toIntExact(xp)));
            save(new PlayerProgressionData(request.playerId(), existing.revision() + 1, updated));
            XpAwardResult result = new XpAwardResult(XpAwardResult.Status.APPLIED, appliedXp,
                    oldLevel, level, "");
            eventBus.publish(new XpAwardedEvent(request, result));
            if (level != oldLevel) {
                eventBus.publish(new LevelChangedEvent(request.playerId(), request.skillId(),
                        oldLevel, level));
            }
            return result;
        }
    }

    @Override
    public Map<NamespacedId, ProgressionSnapshot> queryAll(UUID playerId) {
        PlayerProgressionData data = load(playerId);
        Map<NamespacedId, ProgressionSnapshot> snapshots = new TreeMap<>();
        for (var skill : registry.skills()) {
            StoredSkillProgress progress = data.skills().getOrDefault(skill.id(),
                    new StoredSkillProgress(0, 0));
            snapshots.put(skill.id(), snapshot(playerId, skill.id(), progress));
        }
        return Map.copyOf(snapshots);
    }

    private ProgressionSnapshot snapshot(UUID playerId, NamespacedId skillId,
                                         StoredSkillProgress progress) {
        int toNext = formula.xpToNextLevel(progress.level(), mode, formulaType);
        return new ProgressionSnapshot(playerId, skillId, progress.level(), progress.xp(), toNext);
    }

    private PlayerProgressionData load(UUID playerId) {
        try {
            return store.load(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
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
