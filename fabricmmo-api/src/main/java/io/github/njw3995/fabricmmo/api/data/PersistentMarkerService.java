package io.github.njw3995.fabricmmo.api.data;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Set;

/**
 * World-scoped, namespaced persistent marker storage for addon milestones and replay rejection.
 *
 * <p>Markers are preserved while an addon is absent. Values should be stable identifiers rather
 * than large payloads. Core owns persistence and atomic shutdown flushing.</p>
 */
public interface PersistentMarkerService {
    /**
     * Returns whether the service is bound to the active world.
     *
     * <p>Addon registration runs before a world exists. Read or mutation methods should normally
     * be used from {@code SERVER_STARTING} or later, after this method becomes {@code true}.</p>
     */
    default boolean available() {
        return false;
    }

    /** Adds a marker and returns {@code true} only when it did not already exist. */
    boolean markOnce(NamespacedId markerType, String subject, String value);

    boolean contains(NamespacedId markerType, String subject, String value);

    Set<String> values(NamespacedId markerType, String subject);

    boolean remove(NamespacedId markerType, String subject, String value);

    static PersistentMarkerService unsupported() {
        return Unsupported.INSTANCE;
    }

    enum Unsupported implements PersistentMarkerService {
        INSTANCE;

        @Override
        public boolean available() {
            return false;
        }

        @Override
        public boolean markOnce(NamespacedId markerType, String subject, String value) {
            throw new UnsupportedOperationException(
                    "This FabricMMO API implementation does not support persistent markers");
        }

        @Override
        public boolean contains(NamespacedId markerType, String subject, String value) {
            return false;
        }

        @Override
        public Set<String> values(NamespacedId markerType, String subject) {
            return Set.of();
        }

        @Override
        public boolean remove(NamespacedId markerType, String subject, String value) {
            return false;
        }
    }
}
