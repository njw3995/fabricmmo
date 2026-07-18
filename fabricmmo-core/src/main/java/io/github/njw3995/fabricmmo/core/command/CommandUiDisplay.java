package io.github.njw3995.fabricmmo.core.command;

import io.github.njw3995.fabricmmo.core.ui.UiSettings;
import java.util.List;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Applies the upstream Print/Board/Display_Time command UI policy. */
final class CommandUiDisplay {
    private static final int SIDEBAR_LINE_LIMIT = 15;

    private CommandUiDisplay() { }

    static void configured(
            ServerCommandSource source,
            UiSettings.BoardType type,
            Text boardTitle,
            Text chatHeader,
            List<Text> lines) {
        java.util.ArrayList<Text> chat = new java.util.ArrayList<>();
        if (chatHeader != null) chat.add(chatHeader);
        chat.addAll(lines);
        configured(source, type, boardTitle, chat, lines);
    }

    static void configured(
            ServerCommandSource source,
            UiSettings.BoardType type,
            Text boardTitle,
            List<Text> chatLines,
            List<Text> boardLines) {
        UiSettings ui = SharedCommandUtil.systems().uiConfiguration();
        UiSettings.Board settings = ui.board(type);
        boolean useBoard = settings.enabled()
                && ui.scoreboardsEnabled()
                && source.getEntity() instanceof ServerPlayerEntity;
        boolean useChat = !useBoard || settings.print();
        if (useChat) {
            chatLines.forEach(source::sendMessage);
        }
        if (useBoard) {
            ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
            SharedCommandUtil.systems().scoreboards().show(
                    player, boardTitle, sidebarLines(boardLines), settings.displaySeconds());
        }
    }

    static void skill(
            ServerCommandSource source,
            Text boardTitle,
            List<Text> chatLines,
            List<Text> boardLines) {
        chatLines.forEach(source::sendMessage);
        UiSettings.Board settings = SharedCommandUtil.systems().uiConfiguration()
                .board(UiSettings.BoardType.SKILL);
        if (settings.enabled()
                && SharedCommandUtil.systems().uiConfiguration().scoreboardsEnabled()
                && source.getEntity() instanceof ServerPlayerEntity player) {
            SharedCommandUtil.systems().scoreboards().show(
                    player, boardTitle, sidebarLines(boardLines), settings.displaySeconds());
        }
    }

    private static List<Text> sidebarLines(List<Text> lines) {
        return lines.size() <= SIDEBAR_LINE_LIMIT
                ? List.copyOf(lines)
                : List.copyOf(lines.subList(0, SIDEBAR_LINE_LIMIT));
    }
}
