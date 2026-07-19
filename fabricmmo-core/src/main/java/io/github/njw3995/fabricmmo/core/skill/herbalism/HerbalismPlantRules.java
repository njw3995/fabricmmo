package io.github.njw3995.fabricmmo.core.skill.herbalism;

import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;

/** Pure classification and maturity rules translated from HerbalismManager. */
public final class HerbalismPlantRules {
    private static final Set<String> BIZARRE_AGEABLE = Set.of("cactus", "kelp", "sugar_cane", "bamboo");
    private static final Set<String> TALL_LIMITS = Set.of("cactus", "sugar_cane", "bamboo", "kelp", "kelp_plant", "chorus_plant");

    private HerbalismPlantRules() {
    }

    public static boolean isBizarreAgeable(String path) {
        return BIZARRE_AGEABLE.contains(path);
    }

    public static boolean isMature(BlockState state, String path) {
        IntProperty age = ageProperty(state);
        if (age == null || isBizarreAgeable(path)) {
            return true;
        }
        int value = state.get(age);
        if (path.equals("sweet_berry_bush")) {
            return value >= 2;
        }
        int maximum = age.getValues().stream().mapToInt(Integer::intValue).max().orElse(0);
        return value != 0 && value == maximum;
    }

    public static int replantAge(String path, int stage, boolean greenTerra) {
        if (path.equals("sweet_berry_bush")) {
            return greenTerra || stage >= 2 ? 1 : 0;
        }
        if (path.equals("beetroots") || path.equals("nether_wart")) {
            if (greenTerra || stage > 2) {
                return 2;
            }
            return stage == 2 ? 1 : 0;
        }
        if (path.equals("cocoa")) {
            return stage >= 2 ? 1 : 0;
        }
        return Math.max(0, stage);
    }

    public static boolean limitedTallPlant(String path) {
        return TALL_LIMITS.contains(path);
    }

    @SuppressWarnings("unchecked")
    public static IntProperty ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntProperty age && property.getName().equals("age")) {
                return age;
            }
        }
        return null;
    }
}
