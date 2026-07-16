package io.github.njw3995.fabricmmo.api.config;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;

public interface ConfigRegistryView {
    List<ConfigContribution> contributions();

    List<ConfigContribution> contributionsByOwner(NamespacedId owner);

    boolean frozen();
}
