package io.github.njw3995.fabricmmo.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

class LeaderboardCommandsParityTest {
    @Test
    void upstreamNonChildRankOrderAndSidebarLimitArePinned() throws Exception {
        Field orderField = LeaderboardCommands.class
                .getDeclaredField("UPSTREAM_NON_CHILD_SKILL_ORDER");
        orderField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> order = (List<String>) orderField.get(null);
        assertEquals(List.of(
                "acrobatics", "alchemy", "archery", "axes", "crossbows",
                "excavation", "fishing", "herbalism", "maces", "mining",
                "repair", "swords", "taming", "tridents", "unarmed",
                "woodcutting"), order);

        Field limitField = CommandUiDisplay.class.getDeclaredField("SIDEBAR_LINE_LIMIT");
        limitField.setAccessible(true);
        assertEquals(15, limitField.getInt(null));

        Method normalizePage = LeaderboardCommands.class
                .getDeclaredMethod("normalizePage", int.class);
        normalizePage.setAccessible(true);
        assertEquals(2, normalizePage.invoke(null, -2));
        assertEquals(0, normalizePage.invoke(null, 0));
    }

    @Test
    void upstreamLeaderboardAndRankWordingIsPinned() throws Exception {
        Method topHeader = LeaderboardCommands.class
                .getDeclaredMethod("topChatHeader", NamespacedId.class);
        topHeader.setAccessible(true);
        assertEquals("--mcMMO Power Level Leaderboard--",
                ((Text) topHeader.invoke(null, new Object[] { null })).getString());
        assertEquals("--mcMMO Mining Leaderboard--",
                ((Text) topHeader.invoke(null, NamespacedId.parse("fabricmmo:mining"))).getString());

        Method rankLine = LeaderboardCommands.class
                .getDeclaredMethod("rankChatLine", String.class, int.class);
        rankLine.setAccessible(true);
        assertEquals("Mining - Rank #4",
                ((Text) rankLine.invoke(null, "Mining", 4)).getString());
        assertEquals("Mining - Rank #Unranked",
                ((Text) rankLine.invoke(null, "Mining", 0)).getString());

        Method overallLine = LeaderboardCommands.class
                .getDeclaredMethod("overallRankChatLine", int.class);
        overallLine.setAccessible(true);
        assertEquals("Overall - Rank #Unranked",
                ((Text) overallLine.invoke(null, 0)).getString());
    }
}
