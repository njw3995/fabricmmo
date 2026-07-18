package io.github.njw3995.fabricmmo.core.administration;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ProgressionAdminService {
    private final ProgressionService progression;
    private final SkillRegistryView skills;
    public ProgressionAdminService(ProgressionService progression, SkillRegistryView skills){this.progression=Objects.requireNonNull(progression,"progression");this.skills=Objects.requireNonNull(skills,"skills");}
    public ProgressionSnapshot setLevel(UUID player,NamespacedId skill,int level){return progression.setLevel(player,skill,level);}    
    public ProgressionSnapshot addLevels(UUID player,NamespacedId skill,int levels){return progression.addLevels(player,skill,levels);}    
    public Map<NamespacedId,ProgressionSnapshot> setAll(UUID player,int level){for(var skill:skills.skills())if(!skill.childSkill())progression.setLevel(player,skill.id(),level);return progression.queryAll(player);}    
    public Map<NamespacedId,ProgressionSnapshot> addAll(UUID player,int levels){for(var skill:skills.skills())if(!skill.childSkill())progression.addLevels(player,skill.id(),levels);return progression.queryAll(player);}    
    public Map<NamespacedId,ProgressionSnapshot> resetAll(UUID player){return setAll(player,0);}    
}
