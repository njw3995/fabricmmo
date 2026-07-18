package io.github.njw3995.fabricmmo.core.session;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

public record PlayerSessionState(
        boolean abilityUse,
        boolean notifications,
        boolean levelUpSound,
        boolean debug,
        boolean godMode,
        Map<NamespacedId, Boolean> xpBars) {
    public static final PlayerSessionState DEFAULTS = new PlayerSessionState(true,true,true,false,false,Map.of());
    public PlayerSessionState { xpBars=Map.copyOf(Objects.requireNonNull(xpBars,"xpBars")); }
    public PlayerSessionState withAbilityUse(boolean v){return new PlayerSessionState(v,notifications,levelUpSound,debug,godMode,xpBars);}    
    public PlayerSessionState withNotifications(boolean v){return new PlayerSessionState(abilityUse,v,levelUpSound,debug,godMode,xpBars);}    
    public PlayerSessionState withLevelUpSound(boolean v){return new PlayerSessionState(abilityUse,notifications,v,debug,godMode,xpBars);}    
    public PlayerSessionState withDebug(boolean v){return new PlayerSessionState(abilityUse,notifications,levelUpSound,v,godMode,xpBars);}    
    public PlayerSessionState withGodMode(boolean v){return new PlayerSessionState(abilityUse,notifications,levelUpSound,debug,v,xpBars);}    
    public PlayerSessionState withXpBar(NamespacedId id, boolean enabled){java.util.HashMap<NamespacedId,Boolean> m=new java.util.HashMap<>(xpBars);m.put(id,enabled);return new PlayerSessionState(abilityUse,notifications,levelUpSound,debug,godMode,m);}    
}
