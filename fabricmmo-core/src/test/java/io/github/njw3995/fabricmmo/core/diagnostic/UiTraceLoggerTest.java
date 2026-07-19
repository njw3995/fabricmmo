package io.github.njw3995.fabricmmo.core.diagnostic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

class UiTraceLoggerTest {
    @Test
    void descriptionIncludesPlainTextAndFormattingMetadata() {
        Text message = Text.literal("You require ").formatted(Formatting.GRAY)
                .append(Text.literal("50").formatted(Formatting.YELLOW))
                .append(Text.literal(" more levels of ").formatted(Formatting.GRAY))
                .append(Text.literal("Excavation").formatted(Formatting.DARK_AQUA));

        String description = UiTraceLogger.describe(message);

        assertTrue(description.contains("You require 50 more levels of Excavation"));
        assertTrue(description.contains("color=gray"));
        assertTrue(description.contains("color=yellow"));
        assertTrue(description.contains("color=dark_aqua"));
    }
}
