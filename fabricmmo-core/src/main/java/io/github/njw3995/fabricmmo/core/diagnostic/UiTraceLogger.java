package io.github.njw3995.fabricmmo.core.diagnostic;

import io.github.njw3995.fabricmmo.core.ui.ScoreboardValueLine;
import java.util.List;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Temporary structured trace of player-visible UI used for upstream parity verification. */
public final class UiTraceLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/UI_TRACE");
    private static final StackWalker STACK_WALKER = StackWalker.getInstance();
    private static final ThreadLocal<Integer> COMMAND_MESSAGE_DEPTH =
            ThreadLocal.withInitial(() -> 0);
    private static volatile boolean enabled;

    private UiTraceLogger() {
    }

    public static void configure(boolean traceEnabled) {
        enabled = traceEnabled;
        LOGGER.info("FabricMMO UI trace {}", traceEnabled ? "enabled" : "disabled");
    }

    public static void configuration(
            boolean scoreboardsEnabled,
            boolean rainbows,
            boolean abilityNames,
            boolean blankLinesAboveSkillHeader) {
        if (!enabled) {
            return;
        }
        LOGGER.info(
                "channel=CONFIG scoreboardsEnabled={} rainbows={} abilityNames={} "
                        + "blankLinesAboveSkillHeader={}",
                scoreboardsEnabled,
                rainbows,
                abilityNames,
                blankLinesAboveSkillHeader);
    }

    public static void clear() {
        enabled = false;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void beginCommandMessage(
            ServerPlayerEntity player,
            Text message) {
        COMMAND_MESSAGE_DEPTH.set(COMMAND_MESSAGE_DEPTH.get() + 1);
        if (!enabled) {
            return;
        }
        String source = fabricMmoSource();
        if (source == null) {
            return;
        }
        LOGGER.info(
                "channel=CHAT source={} player={} uuid={} text={}",
                source,
                player.getGameProfile().getName(),
                player.getUuid(),
                describe(message));
    }

    public static void endCommandMessage() {
        int depth = COMMAND_MESSAGE_DEPTH.get();
        if (depth <= 1) {
            COMMAND_MESSAGE_DEPTH.remove();
        } else {
            COMMAND_MESSAGE_DEPTH.set(depth - 1);
        }
    }

    public static void playerMessage(
            ServerPlayerEntity player,
            Text message,
            boolean overlay) {
        if (!enabled || (!overlay && COMMAND_MESSAGE_DEPTH.get() > 0)) {
            return;
        }
        String source = fabricMmoSource();
        if (source == null) {
            return;
        }
        LOGGER.info(
                "channel={} source={} player={} uuid={} text={}",
                overlay ? "ACTION_BAR" : "CHAT",
                source,
                player.getGameProfile().getName(),
                player.getUuid(),
                describe(message));
    }

    public static void sidebar(
            ServerPlayerEntity player,
            Text title,
            List<Text> lines,
            int displaySeconds) {
        if (!enabled) {
            return;
        }
        LOGGER.info(
                "channel=SIDEBAR player={} uuid={} event=SHOW displaySeconds={} title={}",
                player.getGameProfile().getName(),
                player.getUuid(),
                displaySeconds,
                describe(title));
        for (int index = 0; index < lines.size(); index++) {
            LOGGER.info(
                    "channel=SIDEBAR player={} uuid={} row={} label={}",
                    player.getGameProfile().getName(),
                    player.getUuid(),
                    index,
                    describe(lines.get(index)));
        }
    }

    public static void sidebarValues(
            ServerPlayerEntity player,
            Text title,
            List<ScoreboardValueLine> lines,
            int displaySeconds) {
        if (!enabled) {
            return;
        }
        LOGGER.info(
                "channel=SIDEBAR player={} uuid={} event=SHOW_VALUES displaySeconds={} title={}",
                player.getGameProfile().getName(),
                player.getUuid(),
                displaySeconds,
                describe(title));
        for (int index = 0; index < lines.size(); index++) {
            ScoreboardValueLine line = lines.get(index);
            LOGGER.info(
                    "channel=SIDEBAR player={} uuid={} row={} label={} value={} valueColor=red",
                    player.getGameProfile().getName(),
                    player.getUuid(),
                    index,
                    describe(line.label()),
                    line.value());
        }
    }

    public static void sidebarCleared(ServerPlayerEntity player) {
        if (!enabled) {
            return;
        }
        LOGGER.info(
                "channel=SIDEBAR player={} uuid={} event=CLEAR",
                player.getGameProfile().getName(),
                player.getUuid());
    }

    public static void bossBar(
            ServerPlayerEntity player,
            Text title,
            float percent,
            BossBar.Color color,
            BossBar.Style style) {
        if (!enabled) {
            return;
        }
        LOGGER.info(
                "channel=BOSS_BAR player={} uuid={} percent={} color={} style={} title={}",
                player.getGameProfile().getName(),
                player.getUuid(),
                percent,
                color,
                style,
                describe(title));
    }


    private static String fabricMmoSource() {
        return STACK_WALKER.walk(frames -> frames
                .map(StackWalker.StackFrame::getClassName)
                .filter(name -> name.startsWith("io.github.njw3995.fabricmmo."))
                .filter(name -> !name.equals(UiTraceLogger.class.getName()))
                .filter(name -> !name.equals(
                        "io.github.njw3995.fabricmmo.core.mixin.ServerPlayerEntityUiTraceMixin"))
                .filter(name -> !name.equals(
                        "io.github.njw3995.fabricmmo.core.mixin.ServerCommandSourceUiTraceMixin"))
                .findFirst()
                .orElse(null));
    }

    static String describe(Text text) {
        StringBuilder result = new StringBuilder();
        result.append("{plain=").append(quote(text.getString())).append(",segments=[");
        int[] segmentIndex = {0};
        text.visit((style, segment) -> {
            if (!segment.isEmpty()) {
                if (segmentIndex[0]++ > 0) {
                    result.append(',');
                }
                result.append("{text=").append(quote(segment))
                        .append(",style=").append(style(style))
                        .append('}');
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return result.append("]}").toString();
    }

    private static String style(Style style) {
        return "{color=" + (style.getColor() == null ? "null" : style.getColor().getName())
                + ",bold=" + style.isBold()
                + ",italic=" + style.isItalic()
                + ",underlined=" + style.isUnderlined()
                + ",strikethrough=" + style.isStrikethrough()
                + ",obfuscated=" + style.isObfuscated()
                + ",click=" + quote(String.valueOf(style.getClickEvent()))
                + ",hover=" + quote(String.valueOf(style.getHoverEvent()))
                + ",insertion=" + quote(style.getInsertion())
                + ",font=" + quote(String.valueOf(style.getFont()))
                + '}';
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 2).append('\"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '\"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(character);
            }
        }
        return escaped.append('\"').toString();
    }
}
