package io.github.njw3995.fabricmmo.core.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompositeProtectionServiceTest {
    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void evaluatesInPriorityOrderAndRequiresEveryProviderToAllow() {
        List<String> order = new ArrayList<>();
        CompositeProtectionService service = new CompositeProtectionService(allowing("base", order));
        service.registerProtectionProvider(
                NamespacedId.parse("example:low"), 10, allowing("low", order));
        service.registerProtectionProvider(
                NamespacedId.parse("example:high"), 100, denying("high", order));
        service.freeze();

        assertFalse(service.canBreak(PLAYER, "minecraft:overworld", 1, 2, 3));
        assertEquals(List.of("high"), order);
        assertTrue(service.frozen());
        assertThrows(IllegalStateException.class, () -> service.registerProtectionProvider(
                NamespacedId.parse("example:late"), 0, allowing("late", order)));
    }

    @Test
    void duplicateProvidersAreRejectedAndFailuresDeny() {
        CompositeProtectionService duplicate = new CompositeProtectionService(
                allowing("base", new ArrayList<>()));
        NamespacedId id = NamespacedId.parse("example:claims");
        duplicate.registerProtectionProvider(id, 0, allowing("one", new ArrayList<>()));
        assertThrows(IllegalStateException.class,
                () -> duplicate.registerProtectionProvider(
                        id, 1, allowing("two", new ArrayList<>())));

        CompositeProtectionService failing = new CompositeProtectionService(new ProtectionService() {
            @Override
            public boolean canBreak(UUID playerId, String worldId, int x, int y, int z) {
                throw new IllegalStateException("provider failed");
            }

            @Override
            public boolean canModify(UUID playerId, String worldId, int x, int y, int z) {
                return true;
            }

            @Override
            public boolean canInteract(UUID playerId, String worldId, int x, int y, int z) {
                return true;
            }

            @Override
            public boolean canDamage(UUID attackerId, UUID targetId, String worldId) {
                return true;
            }
        });
        assertFalse(failing.canBreak(PLAYER, "minecraft:overworld", 0, 0, 0));
        assertTrue(failing.canModify(PLAYER, "minecraft:overworld", 0, 0, 0));
    }

    private static ProtectionService allowing(String name, List<String> order) {
        return provider(name, order, true);
    }

    private static ProtectionService denying(String name, List<String> order) {
        return provider(name, order, false);
    }

    private static ProtectionService provider(String name, List<String> order, boolean result) {
        return new ProtectionService() {
            @Override
            public boolean canBreak(UUID playerId, String worldId, int x, int y, int z) {
                order.add(name);
                return result;
            }

            @Override
            public boolean canModify(UUID playerId, String worldId, int x, int y, int z) {
                order.add(name);
                return result;
            }

            @Override
            public boolean canInteract(UUID playerId, String worldId, int x, int y, int z) {
                order.add(name);
                return result;
            }

            @Override
            public boolean canDamage(UUID attackerId, UUID targetId, String worldId) {
                order.add(name);
                return result;
            }
        };
    }
}
