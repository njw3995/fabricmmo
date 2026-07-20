package io.github.njw3995.fabricmmo.core.skill.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TamingSettingsTest {
    @TempDir Path temp;

    @Test
    void loadsPinnedRetroDefaultsAndSummonConfiguration() throws Exception {
        Path config = temp.resolve("config.yml");
        Path advanced = temp.resolve("advanced.yml");
        Path ranks = temp.resolve("skillranks.yml");
        Path experience = temp.resolve("experience.yml");
        Files.writeString(config, """
                Skills:
                  Taming:
                    Enabled_For_PVP: true
                    Enabled_For_PVE: true
                    Call_Of_The_Wild:
                      Wolf:
                        Item_Material: BONE
                        Item_Amount: 10
                        Summon_Amount: 1
                        Summon_Length: 240
                        Per_Player_Limit: 2
                """);
        Files.writeString(advanced, """
                Skills:
                  Taming:
                    Gore:
                      Modifier: 2.0
                    FastFoodService:
                      Chance: 50.0
                    ThickFur:
                      Modifier: 2.0
                    ShockProof:
                      Modifier: 6.0
                    SharpenedClaws:
                      Bonus: 2.0
                    Pummel:
                      Chance: 10.0
                    CallOfTheWild:
                      MinHorseJumpStrength: 0.7
                      MaxHorseJumpStrength: 2.0
                """);
        Files.writeString(ranks, """
                Taming:
                  HolyHound:
                    RetroMode:
                      Rank_1: 350
                """);
        Files.writeString(experience, "ExploitFix:\n  COTWBreeding: true\n");

        TamingSettings settings = TamingSettings.load(config, advanced, ranks, experience,
                ProgressionMode.RETRO);
        assertEquals(150, settings.goreUnlock());
        assertEquals(350, settings.holyHoundUnlock());
        assertEquals(750, settings.sharpenedClawsUnlock());
        assertEquals(2.0D, settings.goreModifier());
        assertEquals(50.0D, settings.fastFoodChance());
        assertTrue(settings.pvpEnabled());
        assertTrue(settings.cotwBreedingPrevented());
        TamingSummonSettings wolf = settings.summons().get(TamingSummonType.WOLF);
        assertEquals("minecraft:bone", wolf.itemId().toString());
        assertEquals(10, wolf.itemAmount());
        assertEquals(240, wolf.summonLengthSeconds());
        assertEquals(2, wolf.perPlayerLimit());
    }

    @Test
    void standardModeUsesPinnedUnlockScale() throws Exception {
        Path config = temp.resolve("config.yml");
        Path advanced = temp.resolve("advanced.yml");
        Path ranks = temp.resolve("skillranks.yml");
        Path experience = temp.resolve("experience.yml");
        Files.writeString(config, "Skills:\n  Taming:\n    Enabled_For_PVP: true\n");
        Files.writeString(advanced, "Skills:\n  Taming:\n    Gore:\n      Modifier: 2\n");
        Files.writeString(ranks, "Taming:\n  Gore:\n    Standard:\n      Rank_1: 15\n");
        Files.writeString(experience, "ExploitFix:\n  COTWBreeding: false\n");
        TamingSettings settings = TamingSettings.load(config, advanced, ranks, experience,
                ProgressionMode.STANDARD);
        assertEquals(15, settings.goreUnlock());
        assertEquals(10, settings.environmentallyAwareUnlock());
        assertEquals(75, settings.sharpenedClawsUnlock());
        assertTrue(!settings.cotwBreedingPrevented());
    }
}
