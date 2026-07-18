package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Builds skill guide pages from the pinned mcMMO 2.3.000 locale sections. */
public final class SkillGuideCatalog {
    private final LocaleService locale;

    public SkillGuideCatalog(LocaleService locale) {
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    public List<String> guide(String skill) {
        String normalized = capitalize(skill);
        ArrayList<String> lines = new ArrayList<>();
        for (int section = 0; section < 64; section++) {
            String key = "Guides." + normalized + ".Section." + section;
            if (!locale.contains(key)) {
                continue;
            }
            for (String paragraph : locale.text(key).split("\\R", -1)) {
                if (paragraph.isBlank()) {
                    lines.add("");
                } else {
                    wrap(paragraph, 70, lines);
                }
            }
        }
        return List.copyOf(lines);
    }

    private static void wrap(String text, int width, List<String> output) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                output.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            output.add(line.toString());
        }
    }

    private static String capitalize(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
