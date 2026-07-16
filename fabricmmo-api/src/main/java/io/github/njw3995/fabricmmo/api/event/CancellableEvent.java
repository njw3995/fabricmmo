package io.github.njw3995.fabricmmo.api.event;

public interface CancellableEvent {
    boolean cancelled();

    void cancel();
}
