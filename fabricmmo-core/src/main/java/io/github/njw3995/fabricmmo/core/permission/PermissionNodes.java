package io.github.njw3995.fabricmmo.core.permission;

public final class PermissionNodes {
    public static final String FABRICMMO_ROOT = "fabricmmo.commands.fabricmmo";
    public static final String MCABILITY = "mcmmo.commands.mcability";
    public static final String MCABILITY_OTHERS = "mcmmo.commands.mcability.others";
    public static final String MCCOOLDOWN = "mcmmo.commands.mccooldown";
    public static final String MCLEVELUPSOUND = "mcmmo.commands.mclevelupsound";
    public static final String MCNOTIFY = "mcmmo.commands.mcnotify";
    public static final String MCREFRESH = "mcmmo.commands.mcrefresh";
    public static final String MCREFRESH_OTHERS = "mcmmo.commands.mcrefresh.others";
    public static final String MMOPOWER = "mcmmo.commands.mmopower";
    public static final String ADD_LEVELS = "mcmmo.commands.addlevels";
    public static final String ADD_LEVELS_OTHERS = "mcmmo.commands.addlevels.others";
    public static final String MMO_EDIT = "mcmmo.commands.mmoedit";
    public static final String MMO_EDIT_OTHERS = "mcmmo.commands.mmoedit.others";
    public static final String SKILL_RESET = "mcmmo.commands.skillreset";
    public static final String SKILL_RESET_OTHERS = "mcmmo.commands.skillreset.others";
    public static final String MCMMO_DESCRIPTION = "mcmmo.commands.mcmmo.description";
    public static final String MCMMO_HELP = "mcmmo.commands.mcmmo.help";
    public static final String MCSTATS = "mcmmo.commands.mcstats";
    public static final String ADD_XP = "mcmmo.commands.addxp";
    public static final String ADD_XP_OTHERS = "mcmmo.commands.addxp.others";
    public static final String MINING = "mcmmo.skills.mining";
    public static final String MINING_COMMAND = "mcmmo.commands.mining";
    public static final String MINING_SUPER_BREAKER = "mcmmo.ability.mining.superbreaker";
    public static final String MINING_BLAST_MINING = "mcmmo.ability.mining.blastmining.detonate";
    public static final String MINING_BIGGER_BOMBS = "mcmmo.ability.mining.blastmining.biggerbombs";
    public static final String MINING_DEMOLITIONS_EXPERTISE = "mcmmo.ability.mining.blastmining.demolitionsexpertise";
    public static final String MINING_DOUBLE_DROPS = "mcmmo.ability.mining.doubledrops";
    public static final String MINING_MOTHER_LODE = "mcmmo.ability.mining.motherlode";
    public static final String MINING_LUCKY = "mcmmo.perks.lucky.mining";
    public static final String WOODCUTTING = "mcmmo.skills.woodcutting";
    public static final String WOODCUTTING_COMMAND = "mcmmo.commands.woodcutting";
    public static final String WOODCUTTING_TREE_FELLER =
            "mcmmo.ability.woodcutting.treefeller";
    public static final String WOODCUTTING_HARVEST_LUMBER =
            "mcmmo.ability.woodcutting.harvestlumber";
    public static final String WOODCUTTING_CLEAN_CUTS =
            "mcmmo.ability.woodcutting.cleancuts";
    public static final String WOODCUTTING_KNOCK_ON_WOOD =
            "mcmmo.ability.woodcutting.knockonwood";
    public static final String WOODCUTTING_LEAF_BLOWER =
            "mcmmo.ability.woodcutting.leafblower";
    public static final String WOODCUTTING_LUCKY = "mcmmo.perks.lucky.woodcutting";
    public static final String EXCAVATION = "mcmmo.skills.excavation";
    public static final String EXCAVATION_COMMAND = "mcmmo.commands.excavation";
    public static final String EXCAVATION_GIGA_DRILL_BREAKER =
            "mcmmo.ability.excavation.gigadrillbreaker";
    public static final String EXCAVATION_ARCHAEOLOGY =
            "mcmmo.ability.excavation.archaeology";
    public static final String EXCAVATION_LUCKY = "mcmmo.perks.lucky.excavation";
    public static final String XP_CUSTOM_ALL = "mcmmo.perks.xp.customboost.all";
    public static final String XP_QUADRUPLE_ALL = "mcmmo.perks.xp.quadruple.all";
    public static final String XP_TRIPLE_ALL = "mcmmo.perks.xp.triple.all";
    public static final String XP_150_PERCENT_ALL = "mcmmo.perks.xp.150percentboost.all";
    public static final String XP_DOUBLE_ALL = "mcmmo.perks.xp.double.all";
    public static final String XP_50_PERCENT_ALL = "mcmmo.perks.xp.50percentboost.all";
    public static final String XP_25_PERCENT_ALL = "mcmmo.perks.xp.25percentboost.all";
    public static final String XP_10_PERCENT_ALL = "mcmmo.perks.xp.10percentboost.all";
    public static final String ACTIVATION_TIME_TWELVE_SECONDS =
            "mcmmo.perks.activationtime.twelveseconds";
    public static final String ACTIVATION_TIME_EIGHT_SECONDS =
            "mcmmo.perks.activationtime.eightseconds";
    public static final String ACTIVATION_TIME_FOUR_SECONDS =
            "mcmmo.perks.activationtime.fourseconds";
    public static final String COOLDOWN_HALVED = "mcmmo.perks.cooldowns.halved";
    public static final String COOLDOWN_THIRDED = "mcmmo.perks.cooldowns.thirded";
    public static final String COOLDOWN_QUARTERED = "mcmmo.perks.cooldowns.quartered";

    public static String skill(String skillPath) {
        return "mcmmo.skills." + skillPath;
    }


    private PermissionNodes() {
    }
}
