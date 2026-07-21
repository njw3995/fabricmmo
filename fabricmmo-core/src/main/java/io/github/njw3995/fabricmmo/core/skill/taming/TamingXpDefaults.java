package io.github.njw3995.fabricmmo.core.skill.taming;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TamingXpDefaults {
    private TamingXpDefaults() {}

    public static Map<String, Double> values() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("camel", 1300.0D);
        values.put("sniffer", 1500.0D);
        values.put("llama", 1200.0D);
        values.put("wolf", 250.0D);
        values.put("ocelot", 500.0D);
        values.put("horse", 1000.0D);
        values.put("donkey", 1000.0D);
        values.put("mule", 1000.0D);
        values.put("skeleton_horse", 1000.0D);
        values.put("zombie_horse", 1000.0D);
        values.put("parrot", 1100.0D);
        values.put("cat", 500.0D);
        values.put("fox", 1000.0D);
        values.put("panda", 1000.0D);
        values.put("bee", 100.0D);
        values.put("goat", 250.0D);
        values.put("axolotl", 600.0D);
        values.put("frog", 900.0D);
        return Map.copyOf(values);
    }
}
