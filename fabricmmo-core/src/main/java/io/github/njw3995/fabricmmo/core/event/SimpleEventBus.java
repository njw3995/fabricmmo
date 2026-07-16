package io.github.njw3995.fabricmmo.core.event;

import io.github.njw3995.fabricmmo.api.event.EventSubscription;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SimpleEventBus implements FabricMmoEventBus {
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners =
            new ConcurrentHashMap<>();

    @Override
    public <T> EventSubscription subscribe(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        CopyOnWriteArrayList<Consumer<?>> eventListeners =
                listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>());
        eventListeners.add(listener);
        return () -> eventListeners.remove(listener);
    }

    @Override
    public <T> T publish(T event) {
        Objects.requireNonNull(event, "event");
        for (Consumer<?> listener : listeners.getOrDefault(event.getClass(), new CopyOnWriteArrayList<>())) {
            dispatch(listener, event);
        }
        return event;
    }

    @SuppressWarnings("unchecked")
    private static <T> void dispatch(Consumer<?> listener, T event) {
        ((Consumer<T>) listener).accept(event);
    }
}
