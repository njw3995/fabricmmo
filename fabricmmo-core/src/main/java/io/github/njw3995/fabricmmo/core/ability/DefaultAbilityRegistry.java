package io.github.njw3995.fabricmmo.core.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityRegistrar;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class DefaultAbilityRegistry implements AbilityRegistrar {
    private final SkillRegistryView skills;
    private final Map<NamespacedId, PassiveDefinition> passives = new TreeMap<>();
    private final Map<NamespacedId, ActiveAbilityDefinition> actives = new TreeMap<>();
    private boolean frozen;

    public DefaultAbilityRegistry(SkillRegistryView skills) {
        this.skills = skills;
    }

    @Override
    public synchronized void registerPassive(PassiveDefinition passive) {
        requireOpen();
        requireSkill(passive.skillId());
        if (passives.putIfAbsent(passive.id(), passive) != null || actives.containsKey(passive.id())) {
            throw new IllegalStateException("Duplicate ability id: " + passive.id());
        }
    }

    @Override
    public synchronized void registerActive(ActiveAbilityDefinition active) {
        requireOpen();
        requireSkill(active.skillId());
        if (actives.putIfAbsent(active.id(), active) != null || passives.containsKey(active.id())) {
            throw new IllegalStateException("Duplicate ability id: " + active.id());
        }
    }

    public synchronized Optional<ActiveAbilityDefinition> active(NamespacedId id) {
        return Optional.ofNullable(actives.get(id));
    }

    public synchronized List<ActiveAbilityDefinition> actives() {
        return List.copyOf(actives.values());
    }

    public synchronized Optional<PassiveDefinition> passive(NamespacedId id) {
        return Optional.ofNullable(passives.get(id));
    }

    public synchronized List<PassiveDefinition> passives() {
        return List.copyOf(passives.values());
    }

    public synchronized void freeze() {
        frozen = true;
    }

    private void requireSkill(NamespacedId skillId) {
        if (skills.find(skillId).isEmpty()) {
            throw new IllegalStateException("Ability references unknown skill: " + skillId);
        }
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("Ability registry is frozen");
        }
    }
}
