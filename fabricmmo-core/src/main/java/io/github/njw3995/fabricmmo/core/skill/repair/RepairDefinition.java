package io.github.njw3995.fabricmmo.core.skill.repair;

import java.util.Objects;

public record RepairDefinition(
        String itemName,
        int minimumLevel,
        double xpMultiplier,
        UtilityItemType itemType,
        UtilityMaterialCategory materialCategory,
        String repairMaterialName,
        int minimumQuantity,
        int configuredMaximumDurability) {
    public RepairDefinition {
        itemName = Objects.requireNonNull(itemName, "itemName");
        itemType = Objects.requireNonNull(itemType, "itemType");
        materialCategory = Objects.requireNonNull(materialCategory, "materialCategory");
        repairMaterialName = Objects.requireNonNull(repairMaterialName, "repairMaterialName");
        if (minimumLevel < 0 || minimumQuantity <= 0 || configuredMaximumDurability < 0
                || !Double.isFinite(xpMultiplier) || xpMultiplier < 0.0D) {
            throw new IllegalArgumentException("Invalid repair definition for " + itemName);
        }
    }
}
