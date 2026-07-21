package io.github.njw3995.fabricmmo.api.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Objects;

/** Loader-independent selector for a registry entry or registry tag. */
public record ContentSelector(Kind kind, NamespacedId value) implements Comparable<ContentSelector> {
    public enum Kind {
        ID,
        TAG
    }

    public ContentSelector {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(value, "value");
    }

    public static ContentSelector id(String value) {
        return id(NamespacedId.parse(value));
    }

    public static ContentSelector id(NamespacedId value) {
        return new ContentSelector(Kind.ID, value);
    }

    public static ContentSelector tag(String value) {
        return tag(NamespacedId.parse(value));
    }

    public static ContentSelector tag(NamespacedId value) {
        return new ContentSelector(Kind.TAG, value);
    }

    @Override
    public int compareTo(ContentSelector other) {
        int kindOrder = kind.compareTo(other.kind);
        return kindOrder != 0 ? kindOrder : value.compareTo(other.value);
    }
}
