package io.github.njw3995.fabricmmo.core.ui;

import io.github.njw3995.fabricmmo.core.diagnostic.UiTraceLogger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.FixedNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Packet-per-player sidebar with timed, keepable restoration of the normal server sidebar. */
public final class PlayerScoreboardService {
    private final Map<UUID, Board> boards = new HashMap<>();
    private static final net.minecraft.util.Formatting[] RAINBOW = {
            net.minecraft.util.Formatting.WHITE, net.minecraft.util.Formatting.YELLOW,
            net.minecraft.util.Formatting.LIGHT_PURPLE, net.minecraft.util.Formatting.RED,
            net.minecraft.util.Formatting.AQUA, net.minecraft.util.Formatting.GREEN,
            net.minecraft.util.Formatting.DARK_GRAY, net.minecraft.util.Formatting.BLUE,
            net.minecraft.util.Formatting.DARK_PURPLE, net.minecraft.util.Formatting.DARK_RED,
            net.minecraft.util.Formatting.DARK_AQUA, net.minecraft.util.Formatting.DARK_GREEN,
            net.minecraft.util.Formatting.DARK_BLUE};
    private final Consumer<ServerPlayerEntity> timedBoardCallback;
    private final boolean rainbows;
    private long sequence;

    public PlayerScoreboardService() {
        this(ignored -> { }, false);
    }

    public PlayerScoreboardService(Consumer<ServerPlayerEntity> timedBoardCallback) {
        this(timedBoardCallback, false);
    }

    public PlayerScoreboardService(Consumer<ServerPlayerEntity> timedBoardCallback, boolean rainbows) {
        this.timedBoardCallback = java.util.Objects.requireNonNull(
                timedBoardCallback, "timedBoardCallback");
        this.rainbows = rainbows;
    }

    public synchronized void show(
            ServerPlayerEntity player,
            Text title,
            List<Text> lines,
            int displaySeconds) {
        clear(player);
        java.util.ArrayList<Text> displayedLines = new java.util.ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            displayedLines.add(styledLine(lines.get(index), index));
        }
        UiTraceLogger.sidebar(player, title, displayedLines, displaySeconds);
        long id = ++sequence;
        String rawName = "fmmo" + Long.toString(id, 36)
                + player.getUuidAsString().replace("-", "");
        String objectiveName = rawName.substring(0, Math.min(16, rawName.length()));
        Scoreboard packetScoreboard = new Scoreboard();
        ScoreboardObjective objective = packetScoreboard.addObjective(
                objectiveName, ScoreboardCriterion.DUMMY, title,
                ScoreboardCriterion.RenderType.INTEGER, false, null);
        player.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(
                objective, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));
        player.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(
                ScoreboardDisplaySlot.SIDEBAR, objective));
        int score = lines.size();
        for (int index = 0; index < lines.size(); index++) {
            String holder = "fmmo_line_" + index + '_' + id;
            player.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket(
                    holder, objectiveName, score--, Optional.of(displayedLines.get(index)), Optional.empty()));
        }
        long expiresAt = displaySeconds < 0
                ? Long.MAX_VALUE
                : player.getServer().getTicks() + displaySeconds * 20L;
        boards.put(player.getUuid(), new Board(objective, expiresAt, false));
        if (displaySeconds >= 0) {
            timedBoardCallback.accept(player);
        }
    }



    /** Shows upstream-style label/value rows with red numeric values independent of row order. */
    public synchronized void showValues(
            ServerPlayerEntity player,
            Text title,
            List<ScoreboardValueLine> lines,
            int displaySeconds) {
        clear(player);
        UiTraceLogger.sidebarValues(player, title, lines, displaySeconds);
        long id = ++sequence;
        String rawName = "fmmo" + Long.toString(id, 36)
                + player.getUuidAsString().replace("-", "");
        String objectiveName = rawName.substring(0, Math.min(16, rawName.length()));
        Scoreboard packetScoreboard = new Scoreboard();
        ScoreboardObjective objective = packetScoreboard.addObjective(
                objectiveName, ScoreboardCriterion.DUMMY, title,
                ScoreboardCriterion.RenderType.INTEGER, false, null);
        player.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(
                objective, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));
        player.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(
                ScoreboardDisplaySlot.SIDEBAR, objective));
        int orderingScore = lines.size();
        for (int index = 0; index < lines.size(); index++) {
            ScoreboardValueLine line = lines.get(index);
            String holder = "fmmo_line_" + index + '_' + id;
            player.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket(
                    holder,
                    objectiveName,
                    orderingScore--,
                    Optional.of(line.label()),
                    Optional.of(new FixedNumberFormat(
                            Text.literal(Integer.toString(line.value()))
                                    .formatted(net.minecraft.util.Formatting.RED)))));
        }
        long expiresAt = displaySeconds < 0
                ? Long.MAX_VALUE
                : player.getServer().getTicks() + displaySeconds * 20L;
        boards.put(player.getUuid(), new Board(objective, expiresAt, false));
        if (displaySeconds >= 0) {
            timedBoardCallback.accept(player);
        }
    }

    private Text styledLine(Text line, int index) {
        if (!rainbows) return line;
        return line.copy().formatted(RAINBOW[index % RAINBOW.length]);
    }

    public synchronized void tick(MinecraftServer server) {
        long tick = server.getTicks();
        for (UUID id : List.copyOf(boards.keySet())) {
            Board board = boards.get(id);
            if (!board.keep() && tick >= board.expiresAtTick()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
                if (player != null) clear(player); else boards.remove(id);
            }
        }
    }

    public synchronized void clear(ServerPlayerEntity player) {
        Board board = boards.remove(player.getUuid());
        if (board == null) return;
        UiTraceLogger.sidebarCleared(player);
        ScoreboardObjective restore = player.getServer().getScoreboard()
                .getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        player.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(
                ScoreboardDisplaySlot.SIDEBAR, restore));
        player.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(
                board.objective(), ScoreboardObjectiveUpdateS2CPacket.REMOVE_MODE));
    }

    public synchronized boolean hasBoard(UUID id) { return boards.containsKey(id); }

    public synchronized boolean keep(UUID id) {
        Board board = boards.get(id);
        if (board == null) return false;
        boards.put(id, new Board(board.objective(), board.expiresAtTick(), true));
        return true;
    }

    public synchronized boolean setRevertSeconds(UUID id, int seconds, long nowTick) {
        Board board = boards.get(id);
        if (board == null) return false;
        boards.put(id, new Board(board.objective(),
                seconds < 0 ? Long.MAX_VALUE : nowTick + Math.abs((long) seconds) * 20L, false));
        return true;
    }

    public synchronized void remove(ServerPlayerEntity player) { clear(player); }
    public synchronized void clearAll() { boards.clear(); }
    private record Board(ScoreboardObjective objective, long expiresAtTick, boolean keep) { }
}
