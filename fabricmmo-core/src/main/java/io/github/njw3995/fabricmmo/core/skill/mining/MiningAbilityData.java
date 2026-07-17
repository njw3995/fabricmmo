package io.github.njw3995.fabricmmo.core.skill.mining;

/**
 * Persisted Mining cooldown timestamps. Super Breaker stores its deactivation time; Blast
 * Mining stores its activation time, matching the points where upstream starts each cooldown.
 */
public record MiningAbilityData(long superBreakerLastUsed, long blastMiningLastUsed) {
    public static final MiningAbilityData EMPTY = new MiningAbilityData(0L, 0L);

    public MiningAbilityData {
        if (superBreakerLastUsed < 0L || blastMiningLastUsed < 0L) {
            throw new IllegalArgumentException("Cooldown timestamps must not be negative");
        }
    }

    public MiningAbilityData withSuperBreaker(long timestamp) {
        return new MiningAbilityData(timestamp, blastMiningLastUsed);
    }

    public MiningAbilityData withBlastMining(long timestamp) {
        return new MiningAbilityData(superBreakerLastUsed, timestamp);
    }
}
