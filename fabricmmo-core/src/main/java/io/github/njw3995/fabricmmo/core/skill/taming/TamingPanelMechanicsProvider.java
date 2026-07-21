package io.github.njw3995.fabricmmo.core.skill.taming;

import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** mcMMO 2.3.000 Taming command mechanic rows. */
public final class TamingPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final TamingSettings settings;
    private final LocaleService locale;

    public TamingPanelMechanicsProvider(TamingSettings settings, LocaleService locale) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ArrayList<MechanicRow> rows = new ArrayList<>();
        if (level >= settings.goreUnlock()) {
            // The pinned 2.3.000 source applies Gore whenever unlocked; the displayed chance
            // remains the configured 100% maximum at the default cap.
            rows.add(new MechanicRow(locale.text("Taming.Combat.Chance.Gore"), percent(100.0D)));
        }
        if (level >= settings.environmentallyAwareUnlock()) {
            rows.add(new MechanicRow(locale.text("Taming.Ability.Bonus.0"), "Active"));
        }
        if (level >= settings.thickFurUnlock()) {
            rows.add(new MechanicRow(locale.text("Taming.Ability.Bonus.2"),
                    "1/" + number(settings.thickFurModifier())));
        }
        if (level >= settings.shockProofUnlock()) {
            rows.add(new MechanicRow(locale.text("Taming.Ability.Bonus.4"),
                    "1/" + number(settings.shockProofModifier())));
        }
        if (level >= settings.sharpenedClawsUnlock()) {
            rows.add(new MechanicRow(locale.text("Taming.Ability.Bonus.6"),
                    "+" + number(settings.sharpenedClawsBonus())));
        }
        if (level >= settings.fastFoodUnlock()) {
            rows.add(new MechanicRow(locale.text("Taming.Ability.Bonus.8"),
                    percent(settings.fastFoodChance())));
        }
        if (level >= settings.holyHoundUnlock()) {
            rows.add(new MechanicRow(locale.text("Taming.Ability.Bonus.10"), "Active"));
        }
        return List.copyOf(rows);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }
}
