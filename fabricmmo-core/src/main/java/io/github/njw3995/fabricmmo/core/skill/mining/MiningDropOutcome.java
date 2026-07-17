package io.github.njw3995.fabricmmo.core.skill.mining;

public enum MiningDropOutcome {
    NONE(0),
    DOUBLE(1),
    TRIPLE(2);

    private final int bonusItemsToAdd;

    MiningDropOutcome(int bonusItemsToAdd) {
        this.bonusItemsToAdd = bonusItemsToAdd;
    }

    public int bonusItemsToAdd() {
        return bonusItemsToAdd;
    }
}
