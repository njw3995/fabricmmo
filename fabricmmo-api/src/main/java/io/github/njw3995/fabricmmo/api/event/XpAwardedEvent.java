package io.github.njw3995.fabricmmo.api.event;

import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import java.util.Objects;

public record XpAwardedEvent(XpAwardRequest request, XpAwardResult result) {
    public XpAwardedEvent {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");
    }
}
