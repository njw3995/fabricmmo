package io.github.njw3995.fabricmmo.api.command;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Optional;

public interface CommandMetadataRegistryView {
    Optional<CommandMetadata> find(NamespacedId id);

    List<CommandMetadata> commands();

    boolean frozen();
}
