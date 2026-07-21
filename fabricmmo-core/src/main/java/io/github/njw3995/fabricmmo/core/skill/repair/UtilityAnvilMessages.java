package io.github.njw3995.fabricmmo.core.skill.repair;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import net.minecraft.text.Text;

final class UtilityAnvilMessages {
    private UtilityAnvilMessages() {
    }

    static Text text(String key, Object... values) {
        return LegacyText.parse(SharedServerSystems.require().locale().text(key, values));
    }

    static String localizedSkillName(UtilityAnvilConfirmationService.Kind kind) {
        return SharedServerSystems.require().locale().text(
                kind == UtilityAnvilConfirmationService.Kind.REPAIR
                        ? "Repair.Pretty.Name" : "Salvage.Pretty.Name");
    }
}
