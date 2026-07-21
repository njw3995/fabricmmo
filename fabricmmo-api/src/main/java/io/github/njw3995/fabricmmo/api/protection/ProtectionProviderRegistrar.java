package io.github.njw3995.fabricmmo.api.protection;

import io.github.njw3995.fabricmmo.api.NamespacedId;

/** Registers a generic claims or protection adapter without adding that mod to FabricMMO core. */
@FunctionalInterface
public interface ProtectionProviderRegistrar {
    /**
     * Registers a provider. Every registered provider must allow an action for it to proceed.
     * Higher priorities are evaluated first; ties are ordered by provider id.
     */
    void registerProtectionProvider(
            NamespacedId providerId,
            int priority,
            ProtectionService provider);

    static ProtectionProviderRegistrar unsupported() {
        return (providerId, priority, provider) -> {
            throw new UnsupportedOperationException(
                    "This FabricMMO API implementation does not support protection providers");
        };
    }
}
