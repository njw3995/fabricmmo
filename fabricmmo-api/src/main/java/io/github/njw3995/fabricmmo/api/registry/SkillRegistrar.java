package io.github.njw3995.fabricmmo.api.registry;

import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.api.skill.SkillExtension;

public interface SkillRegistrar {
    void registerSkill(SkillDefinition definition);

    void registerExtension(SkillExtension extension);
}
