package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registered mechanic-stat providers with explicit placeholders for not-yet-implemented skills. */
public final class SkillPanelMechanicsCatalog {
    private final Map<NamespacedId, SkillPanelMechanicsProvider> providers = new ConcurrentHashMap<>();

    public SkillPanelMechanicsCatalog() {
        placeholder("acrobatics", "Roll", "Graceful Roll", "Dodge");
        placeholder("alchemy", "Catalysis", "Concoctions");
        placeholder("archery", "Skill Shot", "Daze", "Arrow Retrieval");
        placeholder("axes", "Axe Mastery", "Critical Strikes", "Impact", "Skull Splitter");
        placeholder("crossbows", "Powered Shot", "Crossbows Limit Break", "Trick Shot");
        placeholder("excavation", "Giga Drill Breaker", "Archaeology");
        placeholder("fishing", "Treasure Hunter", "Magic Hunter", "Shake", "Master Angler");
        placeholder("herbalism", "Green Terra", "Green Thumb", "Hylian Luck", "Double Drops", "Verdant Bounty");
        placeholder("maces", "Mace Mastery", "Crush", "Cripple");
        placeholder("repair", "Repair Mastery", "Super Repair", "Arcane Forging");
        placeholder("salvage", "Scrap Collector", "Arcane Salvage");
        placeholder("smelting", "Fuel Efficiency", "Second Smelt", "Vanilla XP Boost", "Flux Mining");
        placeholder("swords", "Rupture", "Counter Attack", "Serrated Strikes");
        placeholder("taming", "Beast Lore", "Call of the Wild", "Environmentally Aware", "Pummel");
        placeholder("tridents", "Impale", "Tridents Limit Break");
        placeholder("unarmed", "Iron Arm", "Arrow Deflect", "Disarm", "Berserk");
        placeholder("woodcutting", "Tree Feller", "Leaf Blower", "Knock on Wood", "Clean Cuts");
    }

    public void register(NamespacedId id, SkillPanelMechanicsProvider provider) {
        providers.put(id, provider);
    }

    public SkillPanelMechanicsProvider provider(NamespacedId id) {
        return providers.getOrDefault(id, (playerId, level) -> List.of());
    }

    private void placeholder(String path, String... mechanics) {
        NamespacedId id = new NamespacedId("fabricmmo", path);
        providers.put(id, SkillPanelMechanicsProvider.placeholder(id, List.of(mechanics)));
    }
}
