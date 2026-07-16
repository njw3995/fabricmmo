package io.github.njw3995.fabricmmo.core.registry;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistrar;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.api.skill.SkillExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class DefaultSkillRegistry implements SkillRegistrar, SkillRegistryView {
    private final Map<NamespacedId, SkillDefinition> skills = new TreeMap<>();
    private final Map<NamespacedId, List<SkillExtension>> extensions = new TreeMap<>();
    private boolean frozen;

    @Override
    public synchronized void registerSkill(SkillDefinition definition) {
        requireOpen();
        SkillDefinition existing = skills.putIfAbsent(definition.id(), definition);
        if (existing != null) {
            throw new IllegalStateException("Duplicate skill id: " + definition.id());
        }
    }

    @Override
    public synchronized void registerExtension(SkillExtension extension) {
        requireOpen();
        if (!skills.containsKey(extension.targetSkill())) {
            throw new IllegalStateException("Cannot extend unregistered skill: " + extension.targetSkill());
        }
        List<SkillExtension> targetExtensions =
                extensions.computeIfAbsent(extension.targetSkill(), ignored -> new ArrayList<>());
        boolean duplicate = targetExtensions.stream()
                .anyMatch(existing -> existing.extensionId().equals(extension.extensionId()));
        if (duplicate) {
            throw new IllegalStateException("Duplicate extension id for " + extension.targetSkill()
                    + ": " + extension.extensionId());
        }
        targetExtensions.add(extension);
        targetExtensions.sort((left, right) -> left.extensionId().compareTo(right.extensionId()));
    }

    public synchronized void freeze() {
        if (frozen) {
            return;
        }
        for (SkillDefinition definition : skills.values()) {
            for (NamespacedId parent : definition.parents()) {
                SkillDefinition parentDefinition = skills.get(parent);
                if (parentDefinition == null) {
                    throw new IllegalStateException("Missing parent " + parent + " for " + definition.id());
                }
                if (parentDefinition.childSkill()) {
                    throw new IllegalStateException("Child skill cannot parent another child skill: " + parent);
                }
            }
        }
        frozen = true;
    }

    @Override
    public synchronized Optional<SkillDefinition> find(NamespacedId id) {
        return Optional.ofNullable(skills.get(id));
    }

    @Override
    public synchronized List<SkillDefinition> skills() {
        return List.copyOf(skills.values());
    }

    @Override
    public synchronized List<SkillExtension> extensions(NamespacedId targetSkill) {
        return Collections.unmodifiableList(new ArrayList<>(
                extensions.getOrDefault(targetSkill, List.of())));
    }

    @Override
    public synchronized boolean frozen() {
        return frozen;
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("Skill registry is frozen");
        }
    }
}
