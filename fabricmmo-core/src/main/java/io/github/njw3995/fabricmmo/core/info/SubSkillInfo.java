package io.github.njw3995.fabricmmo.core.info;

import java.util.Objects;

public record SubSkillInfo(String enumName,String parentSkill,String configName,int ranks,boolean applicable) {
    public SubSkillInfo{Objects.requireNonNull(enumName,"enumName");Objects.requireNonNull(parentSkill,"parentSkill");Objects.requireNonNull(configName,"configName");if(ranks<0)throw new IllegalArgumentException("ranks");}
    public String lookupName(){return configName.replace(" ","");}
    public String wikiSlug(){return configName.replaceAll("([a-z])([A-Z])","$1-$2").toLowerCase(java.util.Locale.ROOT);}    
}
