package io.github.njw3995.fabricmmo.core.skill.woodcutting;

public enum WoodcuttingDropOutcome {
    NONE(0),
    DOUBLE(1),
    TRIPLE(2);

    private final int bonusCopies;

    WoodcuttingDropOutcome(int bonusCopies) {
        this.bonusCopies = bonusCopies;
    }

    public int bonusCopies() {
        return bonusCopies;
    }
}
