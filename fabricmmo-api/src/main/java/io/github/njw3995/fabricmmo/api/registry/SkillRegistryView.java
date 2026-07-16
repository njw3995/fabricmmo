package io.github.njw3995.fabricmmo.api.registry;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.api.skill.SkillExtension;
import java.util.List;
import java.util.Optional;

public interface SkillRegistryView {
    Optional<SkillDefinition> find(NamespacedId id);

    List<SkillDefinition> skills();

    List<SkillExtension> extensions(NamespacedId targetSkill);

    boolean frozen();
}
