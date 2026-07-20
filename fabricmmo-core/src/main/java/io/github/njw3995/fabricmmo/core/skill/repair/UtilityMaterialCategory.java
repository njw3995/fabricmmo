package io.github.njw3995.fabricmmo.core.skill.repair;

import java.util.Locale;

/** Upstream Repair/Salvage material categories and their exact permission suffixes. */
public enum UtilityMaterialCategory {
    STRING,
    LEATHER,
    WOOD,
    STONE,
    COPPER,
    IRON,
    GOLD,
    DIAMOND,
    NETHERITE,
    OTHER;

    public static UtilityMaterialCategory parse(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("NETHER")) {
            normalized = "NETHERITE";
        }
        return valueOf(normalized);
    }

    public String repairPermission() {
        return "mcmmo.ability.repair." + switch (this) {
            case NETHERITE -> "netheriterepair";
            case OTHER -> "othermaterialrepair";
            default -> name().toLowerCase(Locale.ROOT) + "repair";
        };
    }

    public String salvagePermission() {
        return "mcmmo.ability.salvage." + switch (this) {
            case NETHERITE -> "netheritesalvage";
            case OTHER -> "othermaterialsalvage";
            default -> name().toLowerCase(Locale.ROOT) + "salvage";
        };
    }

    public String experienceKey() {
        return switch (this) {
            case NETHERITE -> "Netherite";
            case STRING -> "String";
            case LEATHER -> "Leather";
            case WOOD -> "Wood";
            case STONE -> "Stone";
            case COPPER -> "Copper";
            case IRON -> "Iron";
            case GOLD -> "Gold";
            case DIAMOND -> "Diamond";
            case OTHER -> "Other";
        };
    }
}
