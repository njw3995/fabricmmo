package io.github.njw3995.fabricmmo.core.skill.taming;

import net.minecraft.util.Identifier;

public enum TamingSummonType {
    WOLF("Wolf", Identifier.of("minecraft", "bone")),
    OCELOT("Ocelot", Identifier.of("minecraft", "cod")),
    HORSE("Horse", Identifier.of("minecraft", "apple"));

    private final String configName;
    private final Identifier defaultItemId;

    TamingSummonType(String configName, Identifier defaultItemId) {
        this.configName = configName;
        this.defaultItemId = defaultItemId;
    }

    public String configName() { return configName; }
    public Identifier defaultItemId() { return defaultItemId; }
}
