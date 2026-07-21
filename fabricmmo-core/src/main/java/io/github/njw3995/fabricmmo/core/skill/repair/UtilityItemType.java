package io.github.njw3995.fabricmmo.core.skill.repair;

import java.util.Locale;

/** Upstream Repair/Salvage item categories used only for permission checks. */
public enum UtilityItemType {
    ARMOR,
    TOOL,
    OTHER;

    public static UtilityItemType parse(String value) {
        return value == null || value.isBlank()
                ? OTHER
                : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public String repairPermission() {
        return "mcmmo.ability.repair." + switch (this) {
            case ARMOR -> "armorrepair";
            case TOOL -> "toolrepair";
            case OTHER -> "otherrepair";
        };
    }

    public String salvagePermission() {
        return "mcmmo.ability.salvage." + switch (this) {
            case ARMOR -> "armorsalvage";
            case TOOL -> "toolsalvage";
            case OTHER -> "othersalvage";
        };
    }
}
