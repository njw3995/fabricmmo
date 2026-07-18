package io.github.njw3995.fabricmmo.core.locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe default locale plus runtime-reloadable locale_override.properties support. */
public final class LocaleService {
    private final Properties defaults;
    private final AtomicInteger generation = new AtomicInteger();
    private volatile Properties active;
    private volatile Path overrideFile;

    public LocaleService(Properties properties) {
        defaults = copyOf(Objects.requireNonNull(properties, "properties"));
        active = copyOf(defaults);
    }

    public static LocaleService loadDefault() {
        InputStream in = LocaleService.class.getResourceAsStream(
                "/defaults/locales/locale_en_US.properties");
        if (in == null) throw new IllegalStateException("Missing default locale");
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return new LocaleService(properties);
    }

    public synchronized ReloadResult reload(Path configuredOverride) throws IOException {
        overrideFile = Objects.requireNonNull(configuredOverride, "configuredOverride")
                .toAbsolutePath().normalize();
        Properties merged = copyOf(defaults);
        int overrides = 0;
        if (Files.isRegularFile(overrideFile)) {
            Properties custom = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(
                    overrideFile, StandardCharsets.UTF_8)) {
                custom.load(reader);
            }
            overrides = custom.size();
            merged.putAll(custom);
        }
        active = merged;
        return new ReloadResult(generation.incrementAndGet(), overrides, overrideFile);
    }

    public String text(String key, Object... args) {
        String pattern = active.getProperty(key, key);
        MessageFormat formatter = new MessageFormat("", Locale.US);
        formatter.applyPattern(pattern.replace("'", "''"));
        return formatter.format(args == null ? new Object[0] : args);
    }

    public boolean contains(String key) { return active.containsKey(key); }
    public int generation() { return generation.get(); }
    public Path overrideFile() { return overrideFile; }

    private static Properties copyOf(Properties source) {
        Properties copy = new Properties();
        copy.putAll(source);
        return copy;
    }

    public record ReloadResult(int generation, int overrideCount, Path path) { }
}
