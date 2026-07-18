package io.github.njw3995.fabricmmo.core.party;

import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Optional server runtime bridge for party XP distribution before normal XP processing. */
public final class PartyXpRuntime {
    @FunctionalInterface
    public interface Distributor {
        Optional<XpAwardResult> distribute(
                XpAwardRequest request,
                Function<XpAwardRequest, XpAwardResult> awarder);
    }

    private static volatile Distributor distributor;

    private PartyXpRuntime() {
    }

    public static void install(Distributor value) {
        distributor = Objects.requireNonNull(value, "value");
    }

    public static Optional<XpAwardResult> distribute(
            XpAwardRequest request,
            Function<XpAwardRequest, XpAwardResult> awarder) {
        Distributor active = distributor;
        return active == null ? Optional.empty() : active.distribute(request, awarder);
    }

    public static void clear() {
        distributor = null;
    }
}
