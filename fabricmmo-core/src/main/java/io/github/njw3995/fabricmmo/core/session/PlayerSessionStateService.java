package io.github.njw3995.fabricmmo.core.session;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class PlayerSessionStateService {
    private final Map<UUID,PlayerSessionState> states=new HashMap<>();
    public synchronized PlayerSessionState get(UUID id){return states.getOrDefault(id,PlayerSessionState.DEFAULTS);}    
    public synchronized PlayerSessionState toggleAbility(UUID id){return update(id,s->s.withAbilityUse(!s.abilityUse()));}
    public synchronized PlayerSessionState toggleNotifications(UUID id){return update(id,s->s.withNotifications(!s.notifications()));}
    public synchronized PlayerSessionState toggleLevelUpSound(UUID id){return update(id,s->s.withLevelUpSound(!s.levelUpSound()));}
    public synchronized PlayerSessionState toggleDebug(UUID id){return update(id,s->s.withDebug(!s.debug()));}
    public synchronized PlayerSessionState toggleGodMode(UUID id){return update(id,s->s.withGodMode(!s.godMode()));}
    public synchronized PlayerSessionState setXpBar(UUID id,NamespacedId skill,boolean enabled){return update(id,s->s.withXpBar(skill,enabled));}
    public synchronized void remove(UUID id){states.remove(id);} public synchronized void clear(){states.clear();}
    private PlayerSessionState update(UUID id,UnaryOperator<PlayerSessionState> op){PlayerSessionState n=op.apply(get(id));states.put(id,n);return n;}
}
