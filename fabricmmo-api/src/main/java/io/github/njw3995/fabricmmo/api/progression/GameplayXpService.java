package io.github.njw3995.fabricmmo.api.progression;

/**
 * Server-authoritative XP gateway intended for normal gameplay listeners in external addons.
 *
 * <p>The implementation validates that the player is online and eligible, checks the registered
 * skill and XP-source permissions, enriches the award with FabricMMO XP perks and permission-aware
 * Power Level context, and then delegates to the normal progression pipeline.</p>
 */
@FunctionalInterface
public interface GameplayXpService {
    XpAwardResult awardOnline(XpAwardRequest request);

    static GameplayXpService unsupported() {
        return request -> new XpAwardResult(
                XpAwardResult.Status.REJECTED,
                0,
                0,
                0,
                "This FabricMMO API implementation does not provide online gameplay XP validation");
    }
}
