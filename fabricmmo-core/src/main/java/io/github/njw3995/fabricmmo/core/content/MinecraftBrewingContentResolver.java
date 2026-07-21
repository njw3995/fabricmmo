package io.github.njw3995.fabricmmo.core.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.BrewingContentDefinition;
import io.github.njw3995.fabricmmo.api.content.BrewingContentRegistryView;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/** Resolves completed external brewing transformations against registered item IDs and tags. */
public final class MinecraftBrewingContentResolver {
    private static final Comparator<BrewingContentDefinition> SPECIFICITY =
            Comparator.<BrewingContentDefinition>comparingInt(MinecraftBrewingContentResolver::idSelectors)
                    .reversed()
                    .thenComparing(BrewingContentDefinition::id);

    private final BrewingContentRegistryView registry;

    public MinecraftBrewingContentResolver(BrewingContentRegistryView registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public boolean empty() {
        return registry.definitions().isEmpty();
    }

    public Optional<BrewingContentDefinition> resolve(
            ItemStack ingredient, ItemStack input, ItemStack output) {
        return registry.definitions().stream()
                .filter(definition -> matches(definition.ingredient(), ingredient))
                .filter(definition -> matches(definition.input(), input))
                .filter(definition -> matches(definition.output(), output))
                .sorted(SPECIFICITY)
                .findFirst();
    }

    public boolean mayMatchIngredient(ItemStack ingredient) {
        return registry.definitions().stream()
                .anyMatch(definition -> matches(definition.ingredient(), ingredient));
    }

    public boolean matches(ContentSelector selector, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        NamespacedId itemId = NamespacedId.parse(Registries.ITEM.getId(stack.getItem()).toString());
        if (selector.kind() == ContentSelector.Kind.ID) {
            return selector.value().equals(itemId);
        }
        Identifier tagId = Identifier.tryParse(selector.value().toString());
        return tagId != null && stack.isIn(TagKey.of(RegistryKeys.ITEM, tagId));
    }

    private static int idSelectors(BrewingContentDefinition definition) {
        int count = 0;
        if (definition.ingredient().kind() == ContentSelector.Kind.ID) count++;
        if (definition.input().kind() == ContentSelector.Kind.ID) count++;
        if (definition.output().kind() == ContentSelector.Kind.ID) count++;
        return count;
    }
}
