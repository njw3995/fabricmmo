package io.github.njw3995.fabricmmo.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NamespacedIdTest {
    @Test
    void parsesAndOrdersIdentifiers() {
        assertEquals("fabricmmo:mining", NamespacedId.parse("fabricmmo:mining").toString());
        assertEquals(-1, Integer.signum(NamespacedId.parse("a:z").compareTo(NamespacedId.parse("b:a"))));
    }

    @Test
    void rejectsInvalidIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> NamespacedId.parse("Mining"));
        assertThrows(IllegalArgumentException.class, () -> NamespacedId.parse("fabricmmo:Upper"));
    }
}
