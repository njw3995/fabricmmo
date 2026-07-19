package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Upstream Bonus_Drops.Herbalism material toggles. */
public final class HerbalismDropSettings {
    private final Set<NamespacedId> enabled;

    private HerbalismDropSettings(Set<NamespacedId> enabled) {
        this.enabled = Set.copyOf(enabled);
    }

    public static HerbalismDropSettings load(Path configFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        HashSet<NamespacedId> enabled = new HashSet<>();
        for (NamespacedId id : HerbalismXpDefaults.values().keySet()) {
            String key = configKey(id.path());
            if (config.bool("Bonus_Drops.Herbalism." + key, true)) {
                enabled.add(id);
            }
        }
        return new HerbalismDropSettings(enabled);
    }

    public boolean materialEnabled(NamespacedId blockId) {
        return enabled.contains(blockId);
    }

    private static String configKey(String path) {
        StringBuilder result = new StringBuilder();
        boolean upper = true;
        for (char ch : path.toCharArray()) {
            if (ch == '_') {
                result.append('_');
                upper = true;
            } else {
                result.append(upper ? Character.toUpperCase(ch) : ch);
                upper = false;
            }
        }
        return result.toString();
    }
}
