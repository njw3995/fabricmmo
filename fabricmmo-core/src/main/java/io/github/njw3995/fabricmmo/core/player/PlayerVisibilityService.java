package io.github.njw3995.fabricmmo.core.player;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.network.ServerPlayerEntity;

/** Central visibility bridge used by inspect, party XP/item sharing, chat, and teleport. */
public final class PlayerVisibilityService {
    @FunctionalInterface
    public interface Provider {
        boolean hiddenFrom(ServerPlayerEntity target, ServerPlayerEntity viewer);
    }

    private final List<Provider> providers = new CopyOnWriteArrayList<>();

    public AutoCloseable register(Provider provider) {
        Provider checked = Objects.requireNonNull(provider, "provider");
        providers.add(checked);
        return () -> providers.remove(checked);
    }

    public boolean visibleTo(ServerPlayerEntity target, ServerPlayerEntity viewer) {
        if (target.equals(viewer)) {
            return true;
        }
        if (target.isInvisibleTo(viewer)) {
            return false;
        }
        for (Provider provider : providers) {
            if (provider.hiddenFrom(target, viewer)) {
                return false;
            }
        }
        return true;
    }
}
