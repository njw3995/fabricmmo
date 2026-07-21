package io.github.njw3995.fabricmmo.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiVersionTest {
    @Test
    void comparesOnlyCompatibleMajorVersions() {
        assertTrue(ApiVersion.isAtLeast("1.4", 1, 4));
        assertTrue(ApiVersion.isAtLeast("1.9", 1, 4));
        assertFalse(ApiVersion.isAtLeast("1.3", 1, 4));
        assertFalse(ApiVersion.isAtLeast("2.0", 1, 4));
    }

    @Test
    void rejectsMalformedVersionsWithUsefulErrors() {
        assertThrows(IllegalArgumentException.class, () -> ApiVersion.isAtLeast("1", 1, 4));
        assertThrows(IllegalArgumentException.class, () -> ApiVersion.isAtLeast("1.x", 1, 4));
        assertThrows(IllegalArgumentException.class, () -> ApiVersion.isAtLeast("-1.4", 1, 4));
        assertThrows(IllegalStateException.class,
                () -> ApiVersion.requireAtLeast("1.3", 1, 4, "Test addon"));
    }
}
