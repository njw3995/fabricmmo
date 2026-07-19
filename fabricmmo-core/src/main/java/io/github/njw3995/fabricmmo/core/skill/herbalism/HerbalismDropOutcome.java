package io.github.njw3995.fabricmmo.core.skill.herbalism;

public enum HerbalismDropOutcome {
    NONE(0), DOUBLE(1), TRIPLE(2);

    private final int bonusCopies;

    HerbalismDropOutcome(int bonusCopies) {
        this.bonusCopies = bonusCopies;
    }

    public int bonusCopies() {
        return bonusCopies;
    }
}
