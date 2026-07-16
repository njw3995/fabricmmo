package io.github.njw3995.fabricmmo.api.event;

@FunctionalInterface
public interface EventSubscription extends AutoCloseable {
    @Override
    void close();
}
