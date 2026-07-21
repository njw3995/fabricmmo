package io.github.njw3995.fabricmmo.core.info;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.ability.AbilityCooldownService;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillPanelCooldownCatalogTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void exactUpstreamSkillBoardAbilityRowsCoverAllApplicableSkills() {
        AbilityCooldownService cooldowns = new AbilityCooldownService();
        register(cooldowns, "super_breaker", 31);
        register(cooldowns, "blast_mining", 17);
        register(cooldowns, "giga_drill_breaker", 23);
        register(cooldowns, "tree_feller", 19);

        SkillPanelCooldownCatalog catalog = new SkillPanelCooldownCatalog(
                cooldowns, LocaleService.loadDefault(), true);

        assertRows(catalog, CoreSkills.ACROBATICS);
        assertRows(catalog, CoreSkills.ALCHEMY);
        assertRows(catalog, CoreSkills.ARCHERY);
        assertRows(catalog, CoreSkills.AXES, "Skull Splitter", "0");
        assertRows(catalog, CoreSkills.CROSSBOWS);
        assertRows(catalog, CoreSkills.EXCAVATION, "Giga Drill B..", "23");
        assertRows(catalog, CoreSkills.FISHING);
        assertRows(catalog, CoreSkills.HERBALISM, "Green Terra", "0");
        assertRows(catalog, CoreSkills.MACES);
        assertRows(catalog, CoreSkills.MINING,
                "Super Breaker", "31", "Blast Mining", "17");
        assertRows(catalog, CoreSkills.REPAIR);
        assertRows(catalog, CoreSkills.SALVAGE);
        assertRows(catalog, CoreSkills.SMELTING);
        assertRows(catalog, CoreSkills.SWORDS, "Serrated Str..", "0");
        assertRows(catalog, CoreSkills.TAMING);
        assertRows(catalog, CoreSkills.TRIDENTS);
        assertRows(catalog, CoreSkills.UNARMED, "Berserk", "0");
        assertRows(catalog, CoreSkills.WOODCUTTING, "Tree Feller", "19");
    }

    @Test
    void hiddenAbilityNamesUseTheSingleLocalizedAbilityLabel() {
        SkillPanelCooldownCatalog catalog = new SkillPanelCooldownCatalog(
                new AbilityCooldownService(), LocaleService.loadDefault(), false);

        assertRows(catalog, CoreSkills.EXCAVATION, "Ability", "0");
        assertRows(catalog, CoreSkills.MINING, "Ability", "0", "Ability", "0");
    }

    @Test
    void skillLabelShorteningMatchesLegacyUpstreamRules() {
        assertEquals("WOODCUTTING", SkillPanelService.shortenSkillLabel("WOODCUTTING", false));
        assertEquals("123456789012..", SkillPanelService.shortenSkillLabel(
                "123456789012345", false));
        assertEquals("12345678901234", SkillPanelService.shortenSkillLabel(
                "123456789012345", true));
    }

    private static void register(AbilityCooldownService cooldowns, String path, int seconds) {
        cooldowns.register(new NamespacedId("fabricmmo", path), new AbilityCooldownService.Provider() {
            @Override
            public int remainingSeconds(UUID playerId) {
                return seconds;
            }

            @Override
            public void reset(UUID playerId) {
            }
        });
    }

    private static void assertRows(
            SkillPanelCooldownCatalog catalog,
            NamespacedId skill,
            String... expectedLabelValuePairs) {
        List<SkillPanelCooldownCatalog.CooldownRow> rows = catalog.rows(skill, PLAYER);
        assertEquals(expectedLabelValuePairs.length / 2, rows.size(), skill.toString());
        for (int index = 0; index < rows.size(); index++) {
            assertEquals(expectedLabelValuePairs[index * 2], rows.get(index).label().getString(),
                    skill + " label " + index);
            assertEquals(Integer.parseInt(expectedLabelValuePairs[index * 2 + 1]),
                    rows.get(index).seconds(), skill + " value " + index);
        }
    }
}
