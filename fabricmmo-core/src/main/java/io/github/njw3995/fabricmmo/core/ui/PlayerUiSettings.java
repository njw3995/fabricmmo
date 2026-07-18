package io.github.njw3995.fabricmmo.core.ui;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record PlayerUiSettings(Map<NamespacedId, XpBarMode> xpBars, boolean keepScoreboard) {
    public static final PlayerUiSettings DEFAULTS = new PlayerUiSettings(Map.of(), false);
    public PlayerUiSettings { xpBars = Map.copyOf(Objects.requireNonNull(xpBars, "xpBars")); }
    public XpBarMode xpBar(NamespacedId id) { return xpBars.getOrDefault(id, XpBarMode.AUTO); }
    public PlayerUiSettings withXpBar(NamespacedId id, XpBarMode mode) {
        HashMap<NamespacedId, XpBarMode> copy = new HashMap<>(xpBars);
        if (mode == XpBarMode.AUTO) copy.remove(id); else copy.put(id, mode);
        return new PlayerUiSettings(copy, keepScoreboard);
    }
    public PlayerUiSettings resetXpBars() { return new PlayerUiSettings(Map.of(), keepScoreboard); }
    public PlayerUiSettings withKeepScoreboard(boolean value) { return new PlayerUiSettings(xpBars, value); }
}
