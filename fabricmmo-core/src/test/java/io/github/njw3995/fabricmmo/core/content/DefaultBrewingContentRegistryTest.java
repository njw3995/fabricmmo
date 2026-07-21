package io.github.njw3995.fabricmmo.core.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.BrewingContentDefinition;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultBrewingContentRegistryTest {
    @Test
    void ordersDefinitionsRejectsDuplicatesAndFreezes() {
        DefaultBrewingContentRegistry registry = new DefaultBrewingContentRegistry();
        BrewingContentDefinition later = definition("test:z_recipe", 3);
        BrewingContentDefinition first = definition("test:a_recipe", 1);

        registry.registerBrewingContent(later);
        registry.registerBrewingContent(first);
        registry.freeze();

        assertEquals(first, registry.definitions().getFirst());
        assertEquals(later, registry.find(NamespacedId.parse("test:z_recipe")).orElseThrow());
        assertTrue(registry.frozen());
        assertThrows(IllegalStateException.class,
                () -> registry.registerBrewingContent(definition("test:late", 2)));
    }

    @Test
    void rejectsDuplicateIds() {
        DefaultBrewingContentRegistry registry = new DefaultBrewingContentRegistry();
        BrewingContentDefinition definition = definition("test:recipe", 2);
        registry.registerBrewingContent(definition);

        assertThrows(IllegalStateException.class,
                () -> registry.registerBrewingContent(definition));
    }


    @Test
    void datapackLayerOverridesAndDisablesFrozenJavaDefinitions() {
        DefaultBrewingContentRegistry registry = new DefaultBrewingContentRegistry();
        BrewingContentDefinition code = definition("test:recipe", 1);
        BrewingContentDefinition override = definition("test:recipe", 4);
        registry.registerBrewingContent(code);
        registry.freeze();

        registry.replaceDatapackDefinitions(java.util.List.of(override), java.util.Set.of());
        assertEquals(4, registry.find(code.id()).orElseThrow().stage());

        registry.replaceDatapackDefinitions(java.util.List.of(), java.util.Set.of(code.id()));
        assertTrue(registry.find(code.id()).isEmpty());
    }

    private static BrewingContentDefinition definition(String id, int stage) {
        return new BrewingContentDefinition(
                NamespacedId.parse(id),
                ContentSelector.id("test:ingredient"),
                ContentSelector.id("minecraft:glass_bottle"),
                ContentSelector.id("test:output"),
                stage,
                Map.of());
    }
}
