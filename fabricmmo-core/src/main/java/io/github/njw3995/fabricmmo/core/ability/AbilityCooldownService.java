package io.github.njw3995.fabricmmo.core.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;

/** Registry-backed cooldown reporting/reset so every skill can plug in without command changes. */
public final class AbilityCooldownService {
    private final Map<NamespacedId,Provider> providers=new ConcurrentHashMap<>();
    public void register(NamespacedId id,Provider provider){if(providers.putIfAbsent(Objects.requireNonNull(id,"id"),Objects.requireNonNull(provider,"provider"))!=null)throw new IllegalStateException("Duplicate cooldown provider "+id);}    
    public Map<NamespacedId,Integer> remaining(UUID playerId){TreeMap<NamespacedId,Integer> r=new TreeMap<>();providers.forEach((id,p)->r.put(id,Math.max(0,p.remainingSeconds(playerId))));return Map.copyOf(r);}    
    public void reset(UUID playerId){providers.values().forEach(p->p.reset(playerId));}
    public interface Provider { int remainingSeconds(UUID playerId); void reset(UUID playerId); }
}
