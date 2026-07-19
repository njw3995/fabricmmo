package io.github.njw3995.fabricmmo.core.skill.woodcutting;

/** Pure eligibility result for ordinary Woodcutting block XP. */
public record WoodcuttingXpDecision(Status status, int xp) {
    public enum Status {
        AWARD,
        ZERO_VALUE,
        CREATIVE,
        INVALID_TOOL,
        NO_PERMISSION,
        PROTECTED,
        PLAYER_PLACED
    }

    public WoodcuttingXpDecision {
        if (xp < 0 || (status == Status.AWARD) != (xp > 0)) {
            throw new IllegalArgumentException("Invalid Woodcutting XP decision");
        }
    }

    public static WoodcuttingXpDecision evaluate(
            int configuredXp,
            boolean creative,
            boolean validTool,
            boolean permission,
            boolean protectionAllowed,
            boolean playerPlaced) {
        if (configuredXp <= 0) {
            return new WoodcuttingXpDecision(Status.ZERO_VALUE, 0);
        }
        if (creative) {
            return new WoodcuttingXpDecision(Status.CREATIVE, 0);
        }
        if (!validTool) {
            return new WoodcuttingXpDecision(Status.INVALID_TOOL, 0);
        }
        if (!permission) {
            return new WoodcuttingXpDecision(Status.NO_PERMISSION, 0);
        }
        if (!protectionAllowed) {
            return new WoodcuttingXpDecision(Status.PROTECTED, 0);
        }
        if (playerPlaced) {
            return new WoodcuttingXpDecision(Status.PLAYER_PLACED, 0);
        }
        return new WoodcuttingXpDecision(Status.AWARD, configuredXp);
    }

    public boolean awardsXp() {
        return status == Status.AWARD;
    }
}
