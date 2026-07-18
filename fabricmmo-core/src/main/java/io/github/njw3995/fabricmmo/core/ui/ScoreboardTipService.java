package io.github.njw3995.fabricmmo.core.ui;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Persists and delivers mcMMO's two scoreboard usage tips with per-login pacing. */
public final class ScoreboardTipService {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/ScoreboardTips");
    private final Path file;
    private final int maximumTipCycles;
    private final LocaleService locale;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Properties shown = new Properties();

    public ScoreboardTipService(Path file, int maximumTipCycles, LocaleService locale)
            throws IOException {
        this.file = file.toAbsolutePath().normalize();
        this.maximumTipCycles = Math.max(0, maximumTipCycles);
        this.locale = Objects.requireNonNull(locale, "locale");
        if (Files.isRegularFile(this.file)) {
            try (InputStream input = Files.newInputStream(this.file)) {
                shown.load(input);
            }
        }
    }

    public synchronized void timedBoardShown(ServerPlayerEntity player) {
        if (maximumTipCycles == 0 || tipsShown(player.getUuid()) >= maximumTipCycles) {
            return;
        }
        Session session = sessions.getOrDefault(player.getUuid(), new Session(false, false));
        if (!session.keepShown()) {
            player.sendMessage(LegacyText.parse(locale.text("Commands.Scoreboard.Tip.Keep")));
            sessions.put(player.getUuid(), new Session(true, false));
            return;
        }
        if (!session.clearShown()) {
            player.sendMessage(LegacyText.parse(locale.text("Commands.Scoreboard.Tip.Clear")));
            sessions.put(player.getUuid(), new Session(true, true));
            shown.setProperty(player.getUuidAsString(), Integer.toString(tipsShown(player.getUuid()) + 1));
            try {
                save();
            } catch (IOException exception) {
                LOGGER.warn("Unable to persist scoreboard tip count for {}", player.getUuid(), exception);
            }
        }
    }

    public synchronized int tipsShown(UUID playerId) {
        try {
            return Math.max(0, Integer.parseInt(shown.getProperty(playerId.toString(), "0")));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public synchronized void remove(UUID playerId) {
        sessions.remove(playerId);
    }

    public synchronized void clearSessions() {
        sessions.clear();
    }

    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            shown.store(output, "FabricMMO scoreboard tips shown");
        }
        try {
            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record Session(boolean keepShown, boolean clearShown) { }
}
