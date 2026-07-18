package io.github.njw3995.fabricmmo.core.xprate;

import io.github.njw3995.fabricmmo.api.NamespacedId;

/** Hot-path bridge installed only while a dedicated server runtime is active. */
public final class XpRateRuntime {
    private static volatile XpRateService service;

    private XpRateRuntime() {
    }

    public static void install(XpRateService activeService) {
        service = java.util.Objects.requireNonNull(activeService, "activeService");
    }

    public static void clear() {
        service = null;
    }

    public static double multiplierFor(NamespacedId skillId) {
        XpRateService current = service;
        return current == null ? 1.0D : current.effectiveRate(skillId);
    }
}
