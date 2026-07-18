package io.github.njw3995.fabricmmo.core.party;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Complete persisted party state used by the Fabric-native party implementation. */
public record PartyState(
        String name,
        UUID owner,
        Set<UUID> members,
        Optional<String> password,
        boolean locked,
        int level,
        double xp,
        ShareMode xpShare,
        ShareMode itemShare,
        Set<ItemShareCategory> itemShareCategories,
        Optional<String> alliance) {
    public PartyState {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(owner, "owner");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Party name must not be blank");
        }
        TreeSet<UUID> normalizedMembers = new TreeSet<>(Objects.requireNonNull(members, "members"));
        normalizedMembers.add(owner);
        members = Set.copyOf(normalizedMembers);
        password = Objects.requireNonNull(password, "password").filter(value -> !value.isBlank());
        if (level < 0 || !Double.isFinite(xp) || xp < 0.0D) {
            throw new IllegalArgumentException("Party level and XP must be non-negative and finite");
        }
        xpShare = Objects.requireNonNull(xpShare, "xpShare");
        itemShare = Objects.requireNonNull(itemShare, "itemShare");
        EnumSet<ItemShareCategory> normalizedCategories = EnumSet.noneOf(ItemShareCategory.class);
        normalizedCategories.addAll(Objects.requireNonNull(itemShareCategories, "itemShareCategories"));
        itemShareCategories = Set.copyOf(normalizedCategories);
        alliance = Objects.requireNonNull(alliance, "alliance").filter(value -> !value.isBlank());
    }

    public static PartyState create(String name, UUID owner, Optional<String> password) {
        return new PartyState(
                name, owner, Set.of(owner), password, true, 0, 0.0D, ShareMode.NONE,
                ShareMode.NONE, EnumSet.allOf(ItemShareCategory.class), Optional.empty());
    }

    public PartyState withMembers(Set<UUID> value) {
        return new PartyState(name, owner, value, password, locked, level, xp, xpShare, itemShare,
                itemShareCategories, alliance);
    }

    public PartyState withOwner(UUID value) {
        return new PartyState(name, value, members, password, locked, level, xp, xpShare, itemShare,
                itemShareCategories, alliance);
    }

    public PartyState withPassword(Optional<String> value) {
        return new PartyState(name, owner, members, value, locked, level, xp, xpShare, itemShare,
                itemShareCategories, alliance);
    }

    public PartyState withLocked(boolean value) {
        return new PartyState(name, owner, members, password, value, level, xp, xpShare, itemShare,
                itemShareCategories, alliance);
    }

    public PartyState withProgress(int newLevel, double newXp) {
        return new PartyState(name, owner, members, password, locked, newLevel, newXp, xpShare,
                itemShare, itemShareCategories, alliance);
    }

    public PartyState withXpShare(ShareMode value) {
        return new PartyState(name, owner, members, password, locked, level, xp, value, itemShare,
                itemShareCategories, alliance);
    }

    public PartyState withItemShare(ShareMode value) {
        return new PartyState(name, owner, members, password, locked, level, xp, xpShare, value,
                itemShareCategories, alliance);
    }

    public PartyState withItemShareCategory(ItemShareCategory category, boolean enabled) {
        EnumSet<ItemShareCategory> categories = itemShareCategories.isEmpty()
                ? EnumSet.noneOf(ItemShareCategory.class)
                : EnumSet.copyOf(itemShareCategories);
        if (enabled) {
            categories.add(category);
        } else {
            categories.remove(category);
        }
        return new PartyState(name, owner, members, password, locked, level, xp, xpShare, itemShare,
                categories, alliance);
    }

    public PartyState withAlliance(Optional<String> value) {
        return new PartyState(name, owner, members, password, locked, level, xp, xpShare, itemShare,
                itemShareCategories, value);
    }

    public PartyState withName(String value) {
        return new PartyState(value, owner, members, password, locked, level, xp, xpShare, itemShare,
                itemShareCategories, alliance);
    }
}
