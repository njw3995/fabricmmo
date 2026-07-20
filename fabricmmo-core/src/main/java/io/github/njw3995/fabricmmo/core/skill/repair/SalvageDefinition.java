package io.github.njw3995.fabricmmo.core.skill.repair;

import java.util.Objects;

public record SalvageDefinition(
        String itemName,
        int minimumLevel,
        double xpMultiplier,
        UtilityItemType itemType,
        UtilityMaterialCategory materialCategory,
        String salvageMaterialName,
        int maximumQuantity,
        int configuredMaximumDurability) {
    public SalvageDefinition {
        itemName = Objects.requireNonNull(itemName, "itemName");
        itemType = Objects.requireNonNull(itemType, "itemType");
        materialCategory = Objects.requireNonNull(materialCategory, "materialCategory");
        salvageMaterialName = Objects.requireNonNull(salvageMaterialName, "salvageMaterialName");
        if (minimumLevel < 0 || maximumQuantity <= 0 || configuredMaximumDurability < 0
                || !Double.isFinite(xpMultiplier) || xpMultiplier < 0.0D) {
            throw new IllegalArgumentException("Invalid salvage definition for " + itemName);
        }
    }
}
