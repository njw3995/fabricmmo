package io.github.njw3995.fabricmmo.core.ui;

import java.util.Objects;
import net.minecraft.text.Text;

/** A sidebar label and its independently rendered numeric score value. */
public record ScoreboardValueLine(Text label, int value) {
    public ScoreboardValueLine {
        Objects.requireNonNull(label, "label");
    }
}
