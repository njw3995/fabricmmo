package io.github.njw3995.fabricmmo.core.skill.mining;

public enum MiningDropOutcome {
    NONE(0),
    DOUBLE(1),
    TRIPLE(2);

    private final int bonusCopies;

    MiningDropOutcome(int bonusCopies) {
        this.bonusCopies = bonusCopies;
    }

    public int bonusCopies() {
        return bonusCopies;
    }
}
