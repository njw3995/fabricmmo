package io.github.njw3995.fabricmmo.api.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Optional;

public interface XpSourceRegistryView {
    Optional<XpSourceDefinition> find(NamespacedId id);

    List<XpSourceDefinition> sources();

    List<XpSourceDefinition> sourcesForSkill(NamespacedId skillId);

    boolean frozen();
}
