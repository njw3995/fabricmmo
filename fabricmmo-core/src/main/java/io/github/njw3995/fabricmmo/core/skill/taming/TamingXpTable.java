package io.github.njw3995.fabricmmo.core.skill.taming;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TamingXpTable {
    private final Map<String, Double> values;

    private TamingXpTable(Map<String, Double> values) { this.values = Map.copyOf(values); }

    public static TamingXpTable load(Path experienceFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(experienceFile);
        Map<String, Double> values = new LinkedHashMap<>(TamingXpDefaults.values());
        String prefix = "Experience_Values.Taming.Animal_Taming.";
        config.valuesWithPrefix(prefix).forEach((key, value) -> {
            String path = key.substring(prefix.length()).toLowerCase(Locale.ROOT);
            try {
                double xp = Double.parseDouble(value);
                if (Double.isFinite(xp) && xp >= 0.0D) values.put(path, xp);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid Taming XP value for " + key, exception);
            }
        });
        return new TamingXpTable(values);
    }

    public double xp(String entityPath) {
        return values.getOrDefault(entityPath.toLowerCase(Locale.ROOT), 0.0D);
    }

    public Map<String, Double> values() { return values; }
}
