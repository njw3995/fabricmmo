package io.github.njw3995.fabricmmo.api.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntityXpContentDefinitionTest {
    @Test
    void acceptsIdsTagsAndImmutableMetadata() {
        EntityXpContentDefinition definition = new EntityXpContentDefinition(
                NamespacedId.parse("example:crystal_golem"),
                EntityXpContentDefinition.Scope.COMBAT,
                ContentSelector.tag("example:golems"),
                2.5D,
                Map.of("source", "example"));

        assertEquals(2.5D, definition.xp());
        assertEquals(ContentSelector.Kind.TAG, definition.entity().kind());
        assertThrows(UnsupportedOperationException.class,
                () -> definition.metadata().put("x", "y"));
    }

    @Test
    void rejectsInvalidXp() {
        NamespacedId id = NamespacedId.parse("example:bad");
        ContentSelector entity = ContentSelector.id("example:bad");
        assertThrows(IllegalArgumentException.class,
                () -> EntityXpContentDefinition.of(
                        id, EntityXpContentDefinition.Scope.COMBAT, entity, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> EntityXpContentDefinition.of(
                        id, EntityXpContentDefinition.Scope.TAMING, entity, Double.NaN));
    }
}
