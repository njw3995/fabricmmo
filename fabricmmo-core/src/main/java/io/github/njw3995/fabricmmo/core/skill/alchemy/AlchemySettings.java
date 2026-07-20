package io.github.njw3995.fabricmmo.core.skill.alchemy;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record AlchemySettings(
        ProgressionMode progressionMode,
        boolean enabledForHoppers,
        boolean preventHopperIngredients,
        boolean preventHopperBottles,
        double catalysisMinSpeed,
        double catalysisMaxSpeed,
        int catalysisMaxBonusLevel,
        int catalysisUnlock,
        List<Integer> concoctionUnlocks,
        List<Double> potionXp) {
    public AlchemySettings {
        concoctionUnlocks = List.copyOf(concoctionUnlocks);
        potionXp = List.copyOf(potionXp);
    }

    public static AlchemySettings load(Path configFile, Path advancedFile, Path ranksFile,
                                       Path experienceFile, ProgressionMode mode) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(ranksFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        String rankMode = mode == ProgressionMode.RETRO ? "RetroMode" : "Standard";
        String advancedMode = rankMode;
        ArrayList<Integer> unlocks = new ArrayList<>();
        int[] defaults = mode == ProgressionMode.RETRO
                ? new int[] {0, 100, 200, 350, 500, 750, 900, 1000}
                : new int[] {0, 10, 20, 35, 50, 75, 90, 100};
        for (int rank = 1; rank <= 8; rank++) {
            unlocks.add(ranks.integer("Alchemy.Concoctions." + rankMode + ".Rank_" + rank,
                    defaults[rank - 1]));
        }
        ArrayList<Double> xp = new ArrayList<>();
        double[] xpDefaults = {666.0D, 1111.0D, 1750.0D, 2250.0D, 0.0D};
        for (int stage = 1; stage <= 5; stage++) {
            xp.add(experience.decimal("Experience_Values.Alchemy.Potion_Brewing.Stage_" + stage,
                    xpDefaults[stage - 1]));
        }
        return new AlchemySettings(mode,
                config.bool("Skills.Alchemy.Enabled_for_Hoppers", true),
                config.bool("Skills.Alchemy.Prevent_Hopper_Transfer_Ingredients", false),
                config.bool("Skills.Alchemy.Prevent_Hopper_Transfer_Bottles", false),
                advanced.decimal("Skills.Alchemy.Catalysis.MinSpeed", 1.0D),
                advanced.decimal("Skills.Alchemy.Catalysis.MaxSpeed", 4.0D),
                advanced.integer("Skills.Alchemy.Catalysis.MaxBonusLevel." + advancedMode,
                        mode == ProgressionMode.RETRO ? 1000 : 100),
                ranks.integer("Alchemy.Catalysis." + rankMode + ".Rank_1", 0),
                unlocks, xp);
    }

    public int concoctionsTier(int level) {
        return AlchemyFormula.concoctionsTier(level, concoctionUnlocks);
    }
    public double xpForStage(int stage) {
        return potionXp.get(Math.max(1, Math.min(5, stage)) - 1);
    }
}
