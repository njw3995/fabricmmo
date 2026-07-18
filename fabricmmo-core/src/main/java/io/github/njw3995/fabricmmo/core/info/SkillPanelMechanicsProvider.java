package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.UUID;

/** Extension point for exact mechanic rows once a skill implementation exists. */
@FunctionalInterface
public interface SkillPanelMechanicsProvider {
    List<MechanicRow> rows(UUID playerId, int level);

    record MechanicRow(String label, String value) { }

    static SkillPanelMechanicsProvider placeholder(NamespacedId skillId, List<String> mechanics) {
        return (playerId, level) -> mechanics.stream()
                .map(name -> new MechanicRow(name, "Pending " + skillId.path() + " implementation"))
                .toList();
    }
}
