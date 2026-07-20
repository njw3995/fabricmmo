package io.github.njw3995.fabricmmo.core.skill.smelting;

import java.util.Optional;
import net.minecraft.item.ItemStack;

/** Server-thread bridge between vanilla's static craft helper and the owning furnace instance. */
public final class SmeltingCraftContext {
    private static final ThreadLocal<SmeltingCraftSnapshot> PENDING = new ThreadLocal<>();
    private static final ThreadLocal<SmeltingCraftSnapshot> CAPTURED = new ThreadLocal<>();

    private SmeltingCraftContext() {
    }

    public static void begin(ItemStack source, ItemStack resultBefore) {
        PENDING.remove();
        if (source.isEmpty()) {
            CAPTURED.remove();
            return;
        }
        CAPTURED.set(new SmeltingCraftSnapshot(source, resultBefore.getCount()));
    }

    public static void finish(boolean crafted) {
        SmeltingCraftSnapshot snapshot = CAPTURED.get();
        CAPTURED.remove();
        if (crafted && snapshot != null) {
            PENDING.set(snapshot);
        } else {
            PENDING.remove();
        }
    }

    public static Optional<SmeltingCraftSnapshot> consume() {
        SmeltingCraftSnapshot snapshot = PENDING.get();
        PENDING.remove();
        return Optional.ofNullable(snapshot);
    }
}
