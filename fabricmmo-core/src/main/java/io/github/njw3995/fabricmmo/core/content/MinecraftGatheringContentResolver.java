package io.github.njw3995.fabricmmo.core.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.ContentSelector;
import io.github.njw3995.fabricmmo.api.content.GatheringContentDefinition;
import io.github.njw3995.fabricmmo.api.content.GatheringContentRegistryView;
import io.github.njw3995.fabricmmo.api.content.MaturityRequirement;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

/** Resolves loader-independent API declarations against Minecraft registries. */
public final class MinecraftGatheringContentResolver {
    private static final Comparator<GatheringContentDefinition> SPECIFICITY =
            Comparator.<GatheringContentDefinition, Integer>comparing(definition ->
                    definition.block().kind() == ContentSelector.Kind.ID ? 0 : 1)
                    .thenComparing(GatheringContentDefinition::id);

    private final GatheringContentRegistryView registry;
    private final Map<CacheKey, Optional<GatheringContentDefinition>> cache =
            new ConcurrentHashMap<>();

    public MinecraftGatheringContentResolver(GatheringContentRegistryView registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void clearCache() {
        cache.clear();
    }

    public Optional<GatheringContentDefinition> resolve(
            NamespacedId skillId, BlockState state) {
        NamespacedId blockId = blockId(state);
        CacheKey key = new CacheKey(skillId, blockId);
        return cache.computeIfAbsent(key, ignored -> registry.definitionsForSkill(skillId).stream()
                .filter(definition -> matchesBlock(definition.block(), state, blockId))
                .sorted(SPECIFICITY)
                .findFirst());
    }

    public int xpFor(NamespacedId skillId, BlockState state, int configuredFallback) {
        return resolve(skillId, state).map(GatheringContentDefinition::xp).orElse(configuredFallback);
    }

    public boolean validTool(GatheringContentDefinition definition, ItemStack stack) {
        if (definition.validTools().isEmpty()) {
            return true;
        }
        NamespacedId itemId = NamespacedId.parse(Registries.ITEM.getId(stack.getItem()).toString());
        return definition.validTools().stream().anyMatch(selector -> matchesItem(selector, stack, itemId));
    }

    public boolean mature(GatheringContentDefinition definition, BlockState state) {
        MaturityRequirement maturity = definition.maturity();
        if (maturity.mode() == MaturityRequirement.Mode.ANY) {
            return true;
        }
        IntProperty property = integerProperty(state, maturity.property());
        if (property == null) {
            return false;
        }
        int current = state.get(property);
        return switch (maturity.mode()) {
            case ANY -> true;
            case INTEGER_PROPERTY_MAXIMUM -> current == property.getValues().stream()
                    .mapToInt(Integer::intValue).max().orElse(Integer.MAX_VALUE);
            case INTEGER_PROPERTY_AT_LEAST -> current >= maturity.value();
        };
    }

    public IntProperty integerProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntProperty integer && property.getName().equals(name)) {
                return integer;
            }
        }
        return null;
    }

    public boolean matchesItem(ContentSelector selector, ItemStack stack) {
        NamespacedId itemId = NamespacedId.parse(Registries.ITEM.getId(stack.getItem()).toString());
        return matchesItem(selector, stack, itemId);
    }

    private static boolean matchesBlock(
            ContentSelector selector, BlockState state, NamespacedId blockId) {
        if (selector.kind() == ContentSelector.Kind.ID) {
            return selector.value().equals(blockId);
        }
        Identifier tagId = Identifier.tryParse(selector.value().toString());
        return tagId != null && state.isIn(TagKey.of(RegistryKeys.BLOCK, tagId));
    }

    private static boolean matchesItem(
            ContentSelector selector, ItemStack stack, NamespacedId itemId) {
        if (selector.kind() == ContentSelector.Kind.ID) {
            return selector.value().equals(itemId);
        }
        Identifier tagId = Identifier.tryParse(selector.value().toString());
        return tagId != null && stack.isIn(TagKey.of(RegistryKeys.ITEM, tagId));
    }

    private static NamespacedId blockId(BlockState state) {
        return NamespacedId.parse(Registries.BLOCK.getId(state.getBlock()).toString());
    }

    private record CacheKey(NamespacedId skillId, NamespacedId blockId) {
    }
}
