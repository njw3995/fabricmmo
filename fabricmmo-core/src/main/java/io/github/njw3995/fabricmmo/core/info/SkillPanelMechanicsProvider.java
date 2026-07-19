package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Extension point for exact mechanic rows once a skill implementation exists. */
@FunctionalInterface
public interface SkillPanelMechanicsProvider {
    List<MechanicRow> rows(UUID playerId, int level);

    /**
     * A locale template plus its arguments. Most rows use Ability.Generic.Template; a few
     * upstream skill commands use Ability.Generic.Template.Custom for preformatted content.
     */
    record MechanicRow(String templateKey, Object[] arguments) {
        public MechanicRow {
            Objects.requireNonNull(templateKey, "templateKey");
            arguments = Objects.requireNonNull(arguments, "arguments").clone();
        }

        public MechanicRow(String label, String value) {
            this("Ability.Generic.Template", new Object[]{label, value});
        }

        public static MechanicRow custom(String formattedMessage) {
            return new MechanicRow(
                    "Ability.Generic.Template.Custom",
                    new Object[]{Objects.requireNonNull(formattedMessage, "formattedMessage")});
        }

        @Override
        public Object[] arguments() {
            return arguments.clone();
        }
    }

    static SkillPanelMechanicsProvider placeholder(NamespacedId skillId, List<String> mechanics) {
        return (playerId, level) -> mechanics.stream()
                .map(name -> new MechanicRow(name, "Pending " + skillId.path() + " implementation"))
                .toList();
    }
}
