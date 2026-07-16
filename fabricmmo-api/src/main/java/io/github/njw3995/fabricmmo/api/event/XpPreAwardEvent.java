package io.github.njw3995.fabricmmo.api.event;

import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import java.util.Objects;

public final class XpPreAwardEvent implements CancellableEvent {
    private final XpAwardRequest request;
    private double multiplier = 1.0;
    private boolean cancelled;

    public XpPreAwardEvent(XpAwardRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    public XpAwardRequest request() {
        return request;
    }

    public double multiplier() {
        return multiplier;
    }

    public void multiplier(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("multiplier must be finite and non-negative");
        }
        this.multiplier = multiplier;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
