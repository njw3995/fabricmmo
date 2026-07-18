package io.github.njw3995.fabricmmo.core.ui;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionSnapshot;
import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Upstream-shaped per-skill XP boss bars with session SHOW/HIDE behavior. */
public final class XpBossBarService {
    public static final int HIDE_TICKS = 60;

    private final FabricMmoApi api;
    private final LocaleService locale;
    private final ProgressionSettings progressionSettings;
    private final UiSettings uiSettings;
    private final Map<Key, Entry> bars = new HashMap<>();

    public XpBossBarService(
            FabricMmoApi api,
            LocaleService locale,
            ProgressionSettings progressionSettings,
            UiSettings uiSettings) {
        this.api = Objects.requireNonNull(api, "api");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.progressionSettings = Objects.requireNonNull(progressionSettings, "progressionSettings");
        this.uiSettings = Objects.requireNonNull(uiSettings, "uiSettings");
    }

    public synchronized void show(
            ServerPlayerEntity player,
            ProgressionSnapshot progress,
            UiSettings.XpBar settings,
            XpBarMode mode,
            long nowTick) {
        Key key = new Key(player.getUuid(), progress.skillId());
        Entry old = bars.get(key);
        boolean earlyGame = progressionSettings.earlyGameBoostEnabled() && progress.level() < 1;
        BossBar.Color requestedColor = earlyGame ? BossBar.Color.YELLOW : settings.color();
        ServerBossBar bar;
        if (old == null || old.color() != requestedColor || old.style() != settings.style()) {
            if (old != null) {
                old.bar().clearPlayers();
            }
            bar = new ServerBossBar(Text.empty(), requestedColor, settings.style());
            bar.addPlayer(player);
        } else {
            bar = old.bar();
        }

        if (old == null || old.level() != progress.level()
                || uiSettings.alwaysUpdateXpBarTitles()) {
            bar.setName(title(progress, earlyGame));
        }
        bar.setPercent(progress.xpToNextLevel() <= 0 ? 0.0F
                : Math.max(0.0F, Math.min(
                        1.0F, (float) progress.xp() / progress.xpToNextLevel())));
        bars.put(key, new Entry(
                bar,
                requestedColor,
                settings.style(),
                progress.level(),
                mode == XpBarMode.ALWAYS ? Long.MAX_VALUE : nowTick + HIDE_TICKS));
    }

    private Text title(ProgressionSnapshot progress, boolean earlyGame) {
        if (earlyGame) {
            return LegacyText.parse(locale.text("XPBar.Template.EarlyGameBoost"));
        }
        String skillName = capitalized(progress.skillId().path());
        if (!uiSettings.xpBarExtraDetails()) {
            return LegacyText.parse(locale.text("XPBar." + skillName, progress.level()));
        }
        int powerLevel = api.progression().queryAll(progress.playerId()).entrySet().stream()
                .filter(entry -> api.skillRegistry().find(entry.getKey())
                        .map(skill -> !skill.childSkill())
                        .orElse(false))
                .mapToInt(entry -> entry.getValue().level())
                .sum();
        int percentage = progress.xpToNextLevel() <= 0
                ? 0
                : (int) ((long) progress.xp() * 100L / progress.xpToNextLevel());
        String base = locale.text("XPBar." + skillName, progress.level());
        return LegacyText.parse(locale.text(
                "XPBar.Complex.Template",
                base,
                progress.xp(),
                progress.xpToNextLevel(),
                powerLevel,
                percentage));
    }

    public synchronized void hide(UUID playerId, NamespacedId skillId) {
        Entry removed = bars.remove(new Key(playerId, skillId));
        if (removed != null) {
            removed.bar().clearPlayers();
        }
    }

    public synchronized void hideAll(UUID playerId) {
        remove(playerId);
    }

    public synchronized void tick(MinecraftServer server) {
        long tick = server.getTicks();
        for (Key key : List.copyOf(bars.keySet())) {
            Entry entry = bars.get(key);
            if (tick >= entry.expires()) {
                entry.bar().clearPlayers();
                bars.remove(key);
            }
        }
    }

    public synchronized void remove(UUID player) {
        bars.entrySet().removeIf(entry -> {
            if (entry.getKey().player().equals(player)) {
                entry.getValue().bar().clearPlayers();
                return true;
            }
            return false;
        });
    }

    public synchronized void clear() {
        bars.values().forEach(entry -> entry.bar().clearPlayers());
        bars.clear();
    }

    private static String capitalized(String value) {
        return value.isEmpty()
                ? value
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private record Key(UUID player, NamespacedId skill) { }

    private record Entry(
            ServerBossBar bar,
            BossBar.Color color,
            BossBar.Style style,
            int level,
            long expires) { }
}
