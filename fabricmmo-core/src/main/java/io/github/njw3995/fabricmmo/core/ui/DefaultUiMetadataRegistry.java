package io.github.njw3995.fabricmmo.core.ui;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.ui.UiMetadata;
import io.github.njw3995.fabricmmo.api.ui.UiMetadataRegistrar;
import io.github.njw3995.fabricmmo.api.ui.UiMetadataRegistryView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class DefaultUiMetadataRegistry implements UiMetadataRegistrar, UiMetadataRegistryView {
    private final SkillRegistryView skills;
    private final Map<NamespacedId, UiMetadata> entries = new TreeMap<>();
    private boolean frozen;

    public DefaultUiMetadataRegistry(SkillRegistryView skills) {
        this.skills = skills;
    }

    @Override
    public synchronized void registerUiMetadata(UiMetadata metadata) {
        requireOpen();
        if (skills.find(metadata.skillId()).isEmpty()) {
            throw new IllegalStateException("UI metadata references unknown skill: " + metadata.skillId());
        }
        if (entries.putIfAbsent(metadata.skillId(), metadata) != null) {
            throw new IllegalStateException("Duplicate UI metadata for skill: " + metadata.skillId());
        }
    }

    @Override
    public synchronized Optional<UiMetadata> findForSkill(NamespacedId skillId) {
        return Optional.ofNullable(entries.get(skillId));
    }

    @Override
    public synchronized List<UiMetadata> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized void freeze() {
        frozen = true;
    }

    @Override
    public synchronized boolean frozen() {
        return frozen;
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("UI metadata registry is frozen");
        }
    }
}
