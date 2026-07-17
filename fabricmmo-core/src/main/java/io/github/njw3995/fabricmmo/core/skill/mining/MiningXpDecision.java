package io.github.njw3995.fabricmmo.core.skill.mining;

public record MiningXpDecision(Status status, int xp) {
    public enum Status {
        AWARD,
        CREATIVE_MODE,
        INVALID_TOOL,
        MISSING_PERMISSION,
        PROTECTION_DENIED,
        PLAYER_PLACED,
        NO_CONFIGURED_XP
    }

    public MiningXpDecision {
        if (status == Status.AWARD && xp <= 0) {
            throw new IllegalArgumentException("Award decisions require positive XP");
        }
        if (status != Status.AWARD && xp != 0) {
            throw new IllegalArgumentException("Rejected decisions must contain zero XP");
        }
    }

    public static MiningXpDecision evaluate(
            int configuredXp,
            boolean creativeMode,
            boolean validTool,
            boolean hasPermission,
            boolean protectionAllowed,
            boolean playerPlaced) {
        if (creativeMode) {
            return rejected(Status.CREATIVE_MODE);
        }
        if (!validTool) {
            return rejected(Status.INVALID_TOOL);
        }
        if (!hasPermission) {
            return rejected(Status.MISSING_PERMISSION);
        }
        if (!protectionAllowed) {
            return rejected(Status.PROTECTION_DENIED);
        }
        if (playerPlaced) {
            return rejected(Status.PLAYER_PLACED);
        }
        if (configuredXp <= 0) {
            return rejected(Status.NO_CONFIGURED_XP);
        }
        return new MiningXpDecision(Status.AWARD, configuredXp);
    }

    public boolean awardsXp() {
        return status == Status.AWARD;
    }

    private static MiningXpDecision rejected(Status status) {
        return new MiningXpDecision(status, 0);
    }
}
