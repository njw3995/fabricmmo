package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.util.Objects;

public record MiningBonusDropDecision(Status status, MiningDropContext context) {
    public enum Status {
        ELIGIBLE,
        CREATIVE_MODE,
        INVALID_TOOL,
        MISSING_SKILL_PERMISSION,
        PROTECTION_DENIED,
        PLAYER_PLACED,
        SOURCE_DISABLED,
        SILK_TOUCH_DISABLED,
        DOUBLE_DROPS_LOCKED,
        MISSING_DOUBLE_DROPS_PERMISSION
    }

    public MiningBonusDropDecision {
        Objects.requireNonNull(status, "status");
        if ((status == Status.ELIGIBLE) != (context != null)) {
            throw new IllegalArgumentException("Only eligible decisions may contain a context");
        }
    }

    public static MiningBonusDropDecision evaluate(
            int skillLevel,
            ProgressionMode mode,
            MiningDropSettings settings,
            boolean creativeMode,
            boolean validTool,
            boolean skillPermission,
            boolean protectionAllowed,
            boolean playerPlaced,
            boolean sourceEnabled,
            boolean silkTouch,
            boolean doubleDropsPermission,
            boolean motherLodePermission,
            boolean superBreakerActive,
            boolean lucky) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(settings, "settings");
        if (skillLevel < 0) {
            throw new IllegalArgumentException("skillLevel must not be negative");
        }
        if (creativeMode) {
            return rejected(Status.CREATIVE_MODE);
        }
        if (!validTool) {
            return rejected(Status.INVALID_TOOL);
        }
        if (!skillPermission) {
            return rejected(Status.MISSING_SKILL_PERMISSION);
        }
        if (!protectionAllowed) {
            return rejected(Status.PROTECTION_DENIED);
        }
        if (playerPlaced) {
            return rejected(Status.PLAYER_PLACED);
        }
        if (!sourceEnabled) {
            return rejected(Status.SOURCE_DISABLED);
        }
        if (silkTouch && !settings.silkTouchEnabled()) {
            return rejected(Status.SILK_TOUCH_DISABLED);
        }
        if (!settings.doubleDropsUnlocked(skillLevel, mode)) {
            return rejected(Status.DOUBLE_DROPS_LOCKED);
        }
        if (!doubleDropsPermission) {
            return rejected(Status.MISSING_DOUBLE_DROPS_PERMISSION);
        }

        return new MiningBonusDropDecision(
                Status.ELIGIBLE,
                new MiningDropContext(
                        skillLevel,
                        mode,
                        true,
                        motherLodePermission && settings.motherLodeUnlocked(skillLevel, mode),
                        superBreakerActive,
                        settings.allowSuperBreakerTripleDrops(),
                        lucky));
    }

    public boolean eligible() {
        return status == Status.ELIGIBLE;
    }

    private static MiningBonusDropDecision rejected(Status status) {
        return new MiningBonusDropDecision(status, null);
    }
}
