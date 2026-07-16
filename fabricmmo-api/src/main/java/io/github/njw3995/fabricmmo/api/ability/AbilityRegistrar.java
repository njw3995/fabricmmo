package io.github.njw3995.fabricmmo.api.ability;

public interface AbilityRegistrar {
    void registerPassive(PassiveDefinition passive);

    void registerActive(ActiveAbilityDefinition active);
}
