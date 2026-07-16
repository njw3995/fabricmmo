package io.github.njw3995.fabricmmo.api.event;

import java.util.function.Consumer;

public interface FabricMmoEventBus {
    <T> EventSubscription subscribe(Class<T> eventType, Consumer<T> listener);

    <T> T publish(T event);
}
