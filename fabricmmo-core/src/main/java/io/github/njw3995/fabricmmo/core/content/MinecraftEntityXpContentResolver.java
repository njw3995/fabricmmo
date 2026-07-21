package io.github.njw3995.fabricmmo.core.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentDefinition;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentRegistryView;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/** Resolves generic entity ID/tag declarations against the server's entity-type registry. */
public final class MinecraftEntityXpContentResolver {
    private static final Comparator<EntityXpContentDefinition> SPECIFICITY =
            Comparator.<EntityXpContentDefinition, Integer>comparing(definition ->
                    definition.entity().kind() == ContentSelector.Kind.ID ? 0 : 1)
                    .thenComparing(EntityXpContentDefinition::id);

    private final EntityXpContentRegistryView registry;
    private final Map<CacheKey, Optional<EntityXpContentDefinition>> cache =
            new ConcurrentHashMap<>();

    public MinecraftEntityXpContentResolver(EntityXpContentRegistryView registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public Optional<EntityXpContentDefinition> resolve(
            EntityXpContentDefinition.Scope scope,
            EntityType<?> type) {
        NamespacedId typeId = NamespacedId.parse(Registries.ENTITY_TYPE.getId(type).toString());
        return cache.computeIfAbsent(new CacheKey(scope, typeId), ignored ->
                registry.definitionsForScope(scope).stream()
                        .filter(definition -> matches(definition.entity(), type, typeId))
                        .sorted(SPECIFICITY)
                        .findFirst());
    }

    public void clearCache() {
        cache.clear();
    }

    private static boolean matches(
            ContentSelector selector,
            EntityType<?> type,
            NamespacedId typeId) {
        if (selector.kind() == ContentSelector.Kind.ID) return selector.value().equals(typeId);
        Identifier tagId = Identifier.tryParse(selector.value().toString());
        return tagId != null
                && type.isIn(TagKey.of(RegistryKeys.ENTITY_TYPE, tagId));
    }

    private record CacheKey(EntityXpContentDefinition.Scope scope, NamespacedId typeId) {
    }
}
