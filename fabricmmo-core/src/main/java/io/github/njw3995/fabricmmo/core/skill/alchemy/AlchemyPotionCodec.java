package io.github.njw3995.fabricmmo.core.skill.alchemy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Converts and matches the upstream potion graph against 1.21.1 data components. */
public final class AlchemyPotionCodec {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO");

    private AlchemyPotionCodec() {}

    public static AlchemyPotionDefinition match(AlchemyPotionConfig config, ItemStack stack) {
        if (stack.isEmpty()) return null;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return null;
        Identifier material = Registries.ITEM.getId(stack.getItem());
        Identifier basePotion = contents.potion()
                .map(entry -> Registries.POTION.getId(entry.value())).orElse(null);
        List<EffectSignature> actualEffects = contents.customEffects().stream()
                .map(AlchemyPotionCodec::signature).toList();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        List<String> actualLore = lore == null ? List.of()
                : lore.lines().stream().map(Text::getString).toList();
        AlchemyPotionDefinition match = null;
        for (AlchemyPotionDefinition candidate : config.potions().values()) {
            if (!candidate.materialId().equals(material)) continue;
            if (!basePotionId(candidate).equals(basePotion)) continue;
            if (!multiset(candidate.effects().stream().map(AlchemyPotionCodec::signature).toList())
                    .equals(multiset(actualEffects))) continue;
            if (!candidate.lore().equals(actualLore)) continue;
            if (match != null) {
                LOGGER.warn("Alchemy potion stack matches multiple configured entries: {} and {}; using {}",
                        match.id(), candidate.id(), match.id());
                continue;
            }
            match = candidate;
        }
        return match;
    }

    public static ItemStack create(AlchemyPotionDefinition definition, int amount) {
        if (!Registries.ITEM.containsId(definition.materialId())) {
            throw new IllegalArgumentException("Unknown potion material: " + definition.materialId());
        }
        Item item = Registries.ITEM.get(definition.materialId());
        RegistryEntry<Potion> potion = Registries.POTION.getEntry(basePotionId(definition))
                .<RegistryEntry<Potion>>map(entry -> entry)
                .orElse(Potions.MUNDANE);
        List<StatusEffectInstance> effects = new ArrayList<>();
        for (AlchemyEffectDefinition effect : definition.effects()) {
            var type = Registries.STATUS_EFFECT.getEntry(effect.effectId()).orElse(null);
            if (type == null) throw new IllegalArgumentException("Unknown potion effect: " + effect.effectId());
            effects.add(new StatusEffectInstance(type, effect.duration(), effect.amplifier(),
                    effect.ambient(), effect.showParticles(), effect.showIcon()));
        }
        ItemStack result = new ItemStack(item, Math.max(1, amount));
        result.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                Optional.of(potion), Optional.ofNullable(definition.color()), effects));
        if (definition.name() != null && !definition.name().isBlank()) {
            result.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(definition.name()).setStyle(Style.EMPTY.withItalic(false)));
        }
        if (!definition.lore().isEmpty()) {
            List<Text> lore = definition.lore().stream()
                    .map(line -> (Text) Text.literal(line).setStyle(Style.EMPTY.withItalic(false)))
                    .toList();
            result.set(DataComponentTypes.LORE, new LoreComponent(lore));
        }
        return result;
    }

    public static Identifier basePotionId(AlchemyPotionDefinition definition) {
        String type = switch (definition.potionType().toUpperCase(Locale.ROOT)) {
            case "REGEN" -> "regeneration";
            case "INSTANT_HEAL" -> "healing";
            case "INSTANT_DAMAGE" -> "harming";
            case "JUMP" -> "leaping";
            case "SPEED" -> "swiftness";
            case "UNCRAFTABLE" -> "mundane";
            default -> definition.potionType().toLowerCase(Locale.ROOT);
        };
        if (definition.extended()) type = "long_" + type;
        else if (definition.upgraded()) type = "strong_" + type;
        return Identifier.of("minecraft", type);
    }

    private static EffectSignature signature(AlchemyEffectDefinition effect) {
        return new EffectSignature(effect.effectId(), effect.duration(), effect.amplifier());
    }

    private static EffectSignature signature(StatusEffectInstance effect) {
        return new EffectSignature(Registries.STATUS_EFFECT.getId(effect.getEffectType().value()),
                effect.getDuration(), effect.getAmplifier());
    }

    private static Map<EffectSignature, Integer> multiset(List<EffectSignature> effects) {
        Map<EffectSignature, Integer> result = new HashMap<>();
        for (EffectSignature effect : effects) result.merge(effect, 1, Integer::sum);
        return result;
    }

    private record EffectSignature(Identifier id, int duration, int amplifier) {}
}
