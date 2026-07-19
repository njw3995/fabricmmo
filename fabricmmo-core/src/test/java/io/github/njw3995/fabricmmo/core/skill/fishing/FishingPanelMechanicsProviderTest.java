package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider.MechanicRow;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import java.util.List;
import org.junit.jupiter.api.Test;

class FishingPanelMechanicsProviderTest {
    @Test
    void formatsMasterAnglerValuesInsideTheCustomLocaleTemplates() {
        LocaleService locale = LocaleService.loadDefault();

        List<MechanicRow> rows = FishingPanelMechanicsProvider.masterAnglerRows(
                locale,
                new FishingSettings.WaitBounds(90, 570, 10, 30));

        assertEquals(2, rows.size());
        assertEquals("Ability.Generic.Template.Custom", rows.get(0).templateKey());
        assertEquals("Fishing min wait time reduction: &a-0.5 seconds",
                rows.get(0).arguments()[0]);
        assertEquals("Fishing max wait time reduction: &a-1.5 seconds",
                rows.get(1).arguments()[0]);
    }

    @Test
    void treasureHunterLocaleContainsARealLineBreak() {
        String message = LocaleService.loadDefault().text(
                "Fishing.SubSkill.TreasureHunter.Stat.Extra",
                "7.50%", "1.25%", "0.25%", "0.10%", "0.01%", "0.01%");

        assertTrue(message.contains("\n"));
        assertFalse(message.contains("\\n"));
    }
}
