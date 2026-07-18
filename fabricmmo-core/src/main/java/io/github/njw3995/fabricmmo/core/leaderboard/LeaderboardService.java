package io.github.njw3995.fabricmmo.core.leaderboard;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.persistence.ManagedProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.StoredSkillProgress;
import io.github.njw3995.fabricmmo.core.player.PlayerIdentityStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Backend-neutral leaderboard snapshot service. */
public final class LeaderboardService {
    private final ManagedProgressionStore store;
    private final PlayerIdentityStore identities;
    private final List<NamespacedId> primarySkills;

    public LeaderboardService(
            ManagedProgressionStore store,
            PlayerIdentityStore identities,
            List<NamespacedId> primarySkills) {
        this.store = store;
        this.identities = identities;
        this.primarySkills = List.copyOf(primarySkills);
    }

    public List<LeaderboardEntry> top(NamespacedId skillId, int offset, int limit) {
        return entries(skillId).stream().skip(offset).limit(limit).toList();
    }
    public List<LeaderboardEntry> topPower(int offset, int limit) {
        return entries(null).stream().skip(offset).limit(limit).toList();
    }
    public int rank(UUID playerId, NamespacedId skillId) {
        List<LeaderboardEntry> entries = entries(skillId);
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).playerId().equals(playerId)) return index + 1;
        }
        return 0;
    }

    private List<LeaderboardEntry> entries(NamespacedId skillId) {
        try {
            ArrayList<LeaderboardEntry> result = new ArrayList<>();
            for (UUID id : store.playerIds()) {
                PlayerProgressionData data = store.load(id);
                int power = 0;
                for (NamespacedId skill : primarySkills) {
                    power += data.skills().getOrDefault(skill, new StoredSkillProgress(0, 0)).level();
                }
                int level = skillId == null ? power
                        : data.skills().getOrDefault(skillId, new StoredSkillProgress(0, 0)).level();
                String name = identities.findName(id).orElse(id.toString());
                result.add(new LeaderboardEntry(id, name, level, power));
            }
            Comparator<LeaderboardEntry> order = Comparator
                    .comparingInt(LeaderboardEntry::level).reversed()
                    .thenComparing(LeaderboardEntry::playerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(entry -> entry.playerId().toString());
            result.sort(order);
            return List.copyOf(result);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
