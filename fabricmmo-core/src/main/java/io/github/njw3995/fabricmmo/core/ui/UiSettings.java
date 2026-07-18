package io.github.njw3995.fabricmmo.core.ui;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.entity.boss.BossBar;

/** Upstream scoreboard and experience-bar configuration. */
public final class UiSettings {
    public enum BoardType { RANK, TOP, STATS, INSPECT, COOLDOWN, SKILL }
    public record Board(boolean print, boolean enabled, int displaySeconds) { }
    public record XpBar(boolean enabled, BossBar.Color color, BossBar.Style style) { }

    private final boolean scoreboardsEnabled;
    private final boolean allowKeep;
    private final boolean showStatsAfterLogin;
    private final boolean rainbows;
    private final boolean abilityNames;
    private final int tipsAmount;
    private final Map<BoardType, Board> boards;
    private final boolean xpBarsEnabled;
    private final boolean updatePartyXp;
    private final boolean updatePassiveXp;
    private final boolean alwaysUpdateXpBarTitles;
    private final boolean xpBarExtraDetails;
    private final Map<String, XpBar> xpBars;

    private UiSettings(boolean scoreboardsEnabled, boolean allowKeep, boolean showStatsAfterLogin,
            boolean rainbows, boolean abilityNames, int tipsAmount, Map<BoardType, Board> boards,
            boolean xpBarsEnabled, boolean updatePartyXp, boolean updatePassiveXp,
            boolean alwaysUpdateXpBarTitles, boolean xpBarExtraDetails,
            Map<String, XpBar> xpBars) {
        this.scoreboardsEnabled = scoreboardsEnabled;
        this.allowKeep = allowKeep;
        this.showStatsAfterLogin = showStatsAfterLogin;
        this.rainbows = rainbows;
        this.abilityNames = abilityNames;
        this.tipsAmount = Math.max(0, tipsAmount);
        this.boards = Map.copyOf(boards);
        this.xpBarsEnabled = xpBarsEnabled;
        this.updatePartyXp = updatePartyXp;
        this.updatePassiveXp = updatePassiveXp;
        this.alwaysUpdateXpBarTitles = alwaysUpdateXpBarTitles || xpBarExtraDetails;
        this.xpBarExtraDetails = xpBarExtraDetails;
        this.xpBars = Map.copyOf(xpBars);
    }

    public static UiSettings load(Path configFile, Path experienceFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        Map<BoardType, Board> boards = new java.util.EnumMap<>(BoardType.class);
        boards.put(BoardType.RANK, board(config, "Rank", false, true, 15));
        boards.put(BoardType.TOP, board(config, "Top", true, true, 15));
        boards.put(BoardType.STATS, board(config, "Stats", false, true, 15));
        boards.put(BoardType.INSPECT, board(config, "Inspect", false, true, 20));
        boards.put(BoardType.COOLDOWN, board(config, "Cooldown", true, true, 41));
        boards.put(BoardType.SKILL, new Board(true,
                config.bool("Scoreboard.Types.Skill.Board", true),
                config.integer("Scoreboard.Types.Skill.Display_Time", 30)));
        Map<String, XpBar> bars = new HashMap<>();
        for (String skill : java.util.List.of("Acrobatics", "Alchemy", "Archery", "Axes",
                "Crossbows", "Excavation", "Fishing", "Herbalism", "Mining", "Maces",
                "Repair", "Salvage", "Smelting", "Swords", "Taming", "Tridents",
                "Unarmed", "Woodcutting")) {
            String prefix = "Experience_Bars." + skill + '.';
            bars.put(skill.toLowerCase(Locale.ROOT), new XpBar(
                    experience.bool(prefix + "Enable", true),
                    color(experience.string(prefix + "Color", "GREEN")),
                    style(experience.string(prefix + "BarStyle", "SEGMENTED_6"))));
        }
        return new UiSettings(
                config.bool("Scoreboard.UseScoreboards", false),
                config.bool("Scoreboard.Allow_Keep", true),
                config.bool("Scoreboard.Show_Stats_After_Login", false),
                config.bool("Scoreboard.Rainbows", false),
                config.bool("Scoreboard.Ability_Names", true),
                config.integer("Scoreboard.Tips_Amount", 5),
                boards,
                experience.bool("Experience_Bars.Enable", true),
                experience.bool("Experience_Bars.Update.Party", true),
                experience.bool("Experience_Bars.Update.Passive", true),
                experience.bool(
                        "Experience_Bars.ThisMayCauseLag.AlwaysUpdateTitlesWhenXPIsGained.Enable",
                        false),
                experience.bool(
                        "Experience_Bars.ThisMayCauseLag.AlwaysUpdateTitlesWhenXPIsGained.ExtraDetails",
                        false),
                bars);
    }

    public boolean scoreboardsEnabled() { return scoreboardsEnabled; }
    public boolean allowKeep() { return allowKeep; }
    public boolean showStatsAfterLogin() { return showStatsAfterLogin; }
    public boolean rainbows() { return rainbows; }
    public boolean abilityNames() { return abilityNames; }
    public int tipsAmount() { return tipsAmount; }
    public Board board(BoardType type) { return boards.get(type); }
    public boolean xpBarsEnabled() { return xpBarsEnabled; }
    public boolean updatePartyXp() { return updatePartyXp; }
    public boolean updatePassiveXp() { return updatePassiveXp; }
    public boolean alwaysUpdateXpBarTitles() { return alwaysUpdateXpBarTitles; }
    public boolean xpBarExtraDetails() { return xpBarExtraDetails; }
    public XpBar xpBar(NamespacedId id) {
        return xpBars.getOrDefault(id.path().toLowerCase(Locale.ROOT),
                new XpBar(true, BossBar.Color.GREEN, BossBar.Style.NOTCHED_6));
    }

    private static Board board(FlatYamlConfig config, String type,
            boolean print, boolean enabled, int seconds) {
        String prefix = "Scoreboard.Types." + type + '.';
        return new Board(config.bool(prefix + "Print", print),
                config.bool(prefix + "Board", enabled),
                config.integer(prefix + "Display_Time", seconds));
    }

    private static BossBar.Color color(String value) {
        try { return BossBar.Color.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return BossBar.Color.GREEN; }
    }

    private static BossBar.Style style(String value) {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "SEGMENTED_6", "NOTCHED_6" -> BossBar.Style.NOTCHED_6;
            case "SEGMENTED_10", "NOTCHED_10" -> BossBar.Style.NOTCHED_10;
            case "SEGMENTED_12", "NOTCHED_12" -> BossBar.Style.NOTCHED_12;
            case "SEGMENTED_20", "NOTCHED_20" -> BossBar.Style.NOTCHED_20;
            default -> BossBar.Style.PROGRESS;
        };
    }
}
