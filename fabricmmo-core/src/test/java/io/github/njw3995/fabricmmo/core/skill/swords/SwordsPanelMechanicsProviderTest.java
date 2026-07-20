package io.github.njw3995.fabricmmo.core.skill.swords;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class SwordsPanelMechanicsProviderTest {
    @Test
    void ruptureDisplayPreservesPinnedUpstreamLuckyFormattingQuirk() {
        Properties properties = new Properties();
        properties.setProperty("Perks.Lucky.Bonus", " ({0} with Lucky Perk)");
        LocaleService locale = new LocaleService(properties);

        assertEquals("66.0%", SwordsPanelMechanicsProvider.formatRuptureChance(
                locale, 66.0D, false));
        assertEquals("66.0% (87.78 with Lucky Perk)",
                SwordsPanelMechanicsProvider.formatRuptureChance(locale, 66.0D, true));
    }

    @Test
    void serratedDurationShowsFinalEnduranceLengthLikeUpstream() {
        Properties properties = new Properties();
        properties.setProperty("Perks.ActivationTime.Bonus", " ({0} with Endurance)");
        LocaleService locale = new LocaleService(properties);

        assertEquals("22", SwordsPanelMechanicsProvider.formatSerratedDuration(
                locale, 22, 0));
        assertEquals("22 (34 with Endurance)",
                SwordsPanelMechanicsProvider.formatSerratedDuration(locale, 22, 12));
    }
}
