package io.github.njw3995.fabricmmo.api.ui;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Optional;

public interface UiMetadataRegistryView {
    Optional<UiMetadata> findForSkill(NamespacedId skillId);

    List<UiMetadata> entries();

    boolean frozen();
}
