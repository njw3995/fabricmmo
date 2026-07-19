package io.github.njw3995.fabricmmo.core.skill.excavation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExcavationMessagesTest {
    @Test
    void lockedMessageUsesTheExactUpstreamEnglishWording() {
        assertEquals(
                "You require 50 more levels of Excavation to use this super ability.",
                ExcavationMessages.locked(50).getString());
    }
}
