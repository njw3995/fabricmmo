package io.github.njw3995.fabricmmo.core.skill.repair;

import java.util.Locale;

/** Exact vanilla fallbacks used by upstream when repair/salvage entries omit common fields. */
final class UtilityItemInference {
    private UtilityItemInference() {
    }

    static UtilityItemType itemType(String itemName) {
        String name = itemName.toUpperCase(Locale.ROOT);
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            return UtilityItemType.ARMOR;
        }
        if (name.endsWith("_SWORD") || name.endsWith("_SPEAR")
                || name.endsWith("_SHOVEL") || name.endsWith("_PICKAXE")
                || name.endsWith("_AXE") || name.endsWith("_HOE")
                || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL")
                || name.equals("FISHING_ROD") || name.equals("BOW")
                || name.equals("CROSSBOW") || name.endsWith("_ON_A_STICK")
                || name.equals("TRIDENT") || name.equals("MACE")) {
            return UtilityItemType.TOOL;
        }
        return UtilityItemType.OTHER;
    }

    static UtilityMaterialCategory materialCategory(String itemName) {
        String name = itemName.toUpperCase(Locale.ROOT);
        if (name.startsWith("WOODEN_") || name.equals("SHIELD")) {
            return UtilityMaterialCategory.WOOD;
        }
        if (name.startsWith("STONE_")) {
            return UtilityMaterialCategory.STONE;
        }
        if (name.startsWith("COPPER_")) {
            return UtilityMaterialCategory.COPPER;
        }
        if (name.startsWith("IRON_") || name.equals("SHEARS")
                || name.equals("FLINT_AND_STEEL")) {
            return UtilityMaterialCategory.IRON;
        }
        if (name.startsWith("GOLDEN_")) {
            return UtilityMaterialCategory.GOLD;
        }
        if (name.startsWith("DIAMOND_")) {
            return UtilityMaterialCategory.DIAMOND;
        }
        if (name.startsWith("NETHERITE_")) {
            return UtilityMaterialCategory.NETHERITE;
        }
        if (name.startsWith("LEATHER_")) {
            return UtilityMaterialCategory.LEATHER;
        }
        if (name.equals("FISHING_ROD") || name.equals("BOW")
                || name.equals("CROSSBOW") || name.endsWith("_ON_A_STICK")) {
            return UtilityMaterialCategory.STRING;
        }
        return UtilityMaterialCategory.OTHER;
    }

    static String material(String itemName) {
        return switch (materialCategory(itemName)) {
            case WOOD -> "OAK_PLANKS";
            case STONE -> "COBBLESTONE";
            case COPPER -> "COPPER_INGOT";
            case IRON -> "IRON_INGOT";
            case GOLD -> "GOLD_INGOT";
            case DIAMOND -> "DIAMOND";
            case NETHERITE -> "NETHERITE_SCRAP";
            case LEATHER -> "LEATHER";
            case STRING -> "STRING";
            case OTHER -> switch (itemName.toUpperCase(Locale.ROOT)) {
                case "ELYTRA" -> "PHANTOM_MEMBRANE";
                case "TRIDENT" -> "PRISMARINE_CRYSTALS";
                case "MACE" -> "BREEZE_ROD";
                default -> "AIR";
            };
        };
    }

    static int recipeQuantity(String itemName) {
        String name = itemName.toUpperCase(Locale.ROOT);
        if (name.startsWith("NETHERITE_")) {
            return 4;
        }
        if (name.equals("TRIDENT")) {
            return 16;
        }
        if (name.endsWith("_SWORD") || name.endsWith("_HOE")) {
            return 2;
        }
        if (name.endsWith("_SHOVEL") || name.endsWith("_SPEAR")) {
            return 1;
        }
        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE")) {
            return 3;
        }
        if (name.endsWith("_HELMET")) {
            return 5;
        }
        if (name.endsWith("_CHESTPLATE")) {
            return 8;
        }
        if (name.endsWith("_LEGGINGS")) {
            return 7;
        }
        if (name.endsWith("_BOOTS")) {
            return 4;
        }
        return switch (name) {
            case "SHIELD" -> 6;
            case "SHEARS" -> 2;
            case "FLINT_AND_STEEL" -> 1;
            case "FISHING_ROD" -> 2;
            case "BOW" -> 3;
            case "CARROT_ON_A_STICK" -> 1;
            case "CROSSBOW", "WARPED_FUNGUS_ON_A_STICK" -> 3;
            case "ELYTRA" -> 8;
            case "MACE" -> 4;
            default -> 2;
        };
    }
}
