package io.github.njw3995.fabricmmo.core.access;

import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/** Fabric-native persistent mcMMO Alchemy state attached to a brewing stand. */
public interface AlchemyBrewingStandAccess {
    UUID fabricmmo$alchemyOwner();
    void fabricmmo$setAlchemyOwner(UUID owner);

    DefaultedList<ItemStack> fabricmmo$alchemyInventory();
    int fabricmmo$alchemyBrewTime();
    void fabricmmo$setAlchemyBrewTime(int ticks);
    int fabricmmo$alchemyFuel();
    void fabricmmo$setAlchemyFuel(int fuel);

    boolean fabricmmo$customAlchemyActive();
    double fabricmmo$customAlchemyRemaining();
    double fabricmmo$customAlchemySpeed();
    String fabricmmo$customAlchemyIngredient();
    void fabricmmo$beginCustomAlchemy(double remaining, double speed, String ingredientId);
    void fabricmmo$setCustomAlchemyRemaining(double remaining);
    void fabricmmo$clearCustomAlchemy();
}
