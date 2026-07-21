package io.github.njw3995.fabricmmo.core.skill.repair;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.item.ItemStack;

/** Three-second, item-identity-bound Repair/Salvage confirmations from mcMMO 2.3.000. */
public final class UtilityAnvilConfirmationService {
    public enum Kind {
        REPAIR,
        SALVAGE
    }

    private static final Duration WINDOW = Duration.ofSeconds(3);
    private static final UtilityAnvilConfirmationService GLOBAL =
            new UtilityAnvilConfirmationService(Clock.systemUTC());

    private final TimedIdentityConfirmation<Kind, ItemStack, ItemStack> confirmations;

    public UtilityAnvilConfirmationService(Clock clock) {
        confirmations = new TimedIdentityConfirmation<>(
                Objects.requireNonNull(clock, "clock"),
                WINDOW,
                ItemStack::copy,
                UtilityAnvilConfirmationService::sameItemAndComponents);
    }

    public static UtilityAnvilConfirmationService global() {
        return GLOBAL;
    }

    /** Returns true only when the same item was prompted within the confirmation window. */
    public boolean confirmOrPrompt(UUID playerId, Kind kind, ItemStack item) {
        return confirmations.confirmOrPrompt(playerId, kind, item);
    }

    public boolean isAwaiting(UUID playerId, Kind kind, ItemStack item) {
        return confirmations.isAwaiting(playerId, kind, item);
    }

    /** Rebinds a confirmed Repair to the partially repaired form of the same stack. */
    public void rebind(UUID playerId, Kind kind, ItemStack item) {
        confirmations.rebind(playerId, kind, item);
    }

    public boolean blocksItemUse(UUID playerId, ItemStack item) {
        return confirmations.blocksItemUse(playerId, item);
    }

    public boolean cancel(UUID playerId, Kind kind) {
        return confirmations.cancel(playerId, kind);
    }

    public void clear(UUID playerId) {
        confirmations.clear(playerId);
    }

    public void clearAll() {
        confirmations.clearAll();
    }

    private static boolean sameItemAndComponents(ItemStack prompted, ItemStack current) {
        return !prompted.isEmpty() && !current.isEmpty()
                && ItemStack.areItemsAndComponentsEqual(prompted, current);
    }
}
