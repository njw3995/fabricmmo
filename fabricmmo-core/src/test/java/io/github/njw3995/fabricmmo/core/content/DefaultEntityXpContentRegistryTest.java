package io.github.njw3995.fabricmmo.core.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentDefinition;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultEntityXpContentRegistryTest {
    private static final NamespacedId ID = NamespacedId.parse("example:wolf");

    @Test
    void datapacksOverrideOrDisableJavaDefinitionsAfterFreeze() {
        DefaultEntityXpContentRegistry registry = new DefaultEntityXpContentRegistry();
        registry.registerEntityXpContent(EntityXpContentDefinition.of(
                ID,
                EntityXpContentDefinition.Scope.TAMING,
                ContentSelector.id("minecraft:wolf"),
                250.0D));
        registry.freeze();

        assertTrue(registry.frozen());
        assertEquals(250.0D, registry.find(ID).orElseThrow().xp());
        assertThrows(IllegalStateException.class, () -> registry.registerEntityXpContent(
                EntityXpContentDefinition.of(
                        NamespacedId.parse("example:late"),
                        EntityXpContentDefinition.Scope.COMBAT,
                        ContentSelector.id("minecraft:zombie"),
                        1.0D)));

        registry.replaceDatapackDefinitions(List.of(), Set.of(ID));
        assertTrue(registry.find(ID).isEmpty());

        registry.replaceDatapackDefinitions(List.of(EntityXpContentDefinition.of(
                ID,
                EntityXpContentDefinition.Scope.COMBAT,
                ContentSelector.tag("example:hostiles"),
                3.5D)), Set.of());
        assertEquals(EntityXpContentDefinition.Scope.COMBAT,
                registry.find(ID).orElseThrow().scope());
        assertEquals(1, registry.definitionsForScope(
                EntityXpContentDefinition.Scope.COMBAT).size());
        assertTrue(registry.definitionsForScope(
                EntityXpContentDefinition.Scope.TAMING).isEmpty());
    }

    @Test
    void rejectsDuplicateJavaAndDatapackIds() {
        DefaultEntityXpContentRegistry registry = new DefaultEntityXpContentRegistry();
        EntityXpContentDefinition definition = EntityXpContentDefinition.of(
                ID,
                EntityXpContentDefinition.Scope.COMBAT,
                ContentSelector.id("minecraft:wolf"),
                1.0D);
        registry.registerEntityXpContent(definition);
        assertThrows(IllegalStateException.class,
                () -> registry.registerEntityXpContent(definition));
        assertThrows(IllegalStateException.class,
                () -> registry.replaceDatapackDefinitions(
                        List.of(definition, definition), Set.of()));
    }
}
