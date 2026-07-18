package io.github.njw3995.fabricmmo.core.party;

import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.progression.XpCurve;
import io.github.njw3995.fabricmmo.core.progression.ProgressionFormula;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistent upstream-shaped party ownership, progression, feature, sharing, and alliance service. */
public final class PartyService {
    private final PartyStore store;
    private final PartySettings settings;
    private final ProgressionFormula formula;
    private final ProgressionMode progressionMode;
    private final FormulaType formulaType;
    private final Map<String, PartyState> parties = new HashMap<>();
    private final Map<UUID, String> membership = new HashMap<>();
    private final Map<String, String> pendingAllianceInvites = new HashMap<>();
    private final Map<UUID, String> pendingMemberInvites = new HashMap<>();

    public PartyService(PartyStore store) throws IOException {
        this(store, PartySettings.upstreamDefaults(), new ProgressionFormula(XpCurve.upstreamDefaults()),
                ProgressionMode.RETRO, FormulaType.LINEAR);
    }

    public PartyService(
            PartyStore store,
            PartySettings settings,
            ProgressionFormula formula,
            ProgressionMode progressionMode,
            FormulaType formulaType) throws IOException {
        this.store = Objects.requireNonNull(store, "store");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.formula = Objects.requireNonNull(formula, "formula");
        this.progressionMode = Objects.requireNonNull(progressionMode, "progressionMode");
        this.formulaType = Objects.requireNonNull(formulaType, "formulaType");
        store.load().values().forEach(this::index);
        repairAlliances();
    }

    public synchronized PartySettings settings() {
        return settings;
    }

    public synchronized Optional<PartyState> party(String name) {
        return Optional.ofNullable(parties.get(key(name)));
    }

    public synchronized Optional<PartyState> partyOf(UUID playerId) {
        String party = membership.get(playerId);
        return party == null ? Optional.empty() : Optional.ofNullable(parties.get(party));
    }

    public synchronized java.util.List<PartyState> parties() {
        return parties.values().stream()
                .sorted(Comparator.comparing(PartyState::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized boolean featureUnlocked(PartyState party, PartyFeature feature) {
        return settings.unlocked(party, feature);
    }

    public synchronized int xpToNextLevel(PartyState party, int onlineMemberCount) {
        int base = formula.xpToNextLevel(party.level(), progressionMode, formulaType);
        long multiplier = Math.max(0, onlineMemberCount) + (long) settings.xpCurveModifier();
        long required = base * multiplier;
        return required >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) required;
    }

    public synchronized Result create(UUID owner, String name, Optional<String> password) {
        Objects.requireNonNull(owner, "owner");
        if (membership.containsKey(owner)) {
            return Result.fail("You are already in a party");
        }
        String normalized = validateName(name);
        if (parties.containsKey(key(normalized))) {
            return Result.fail("A party with that name already exists");
        }
        PartyState state = PartyState.create(normalized, owner, password);
        index(state);
        save();
        return Result.ok(state);
    }

    public synchronized Result inviteMember(UUID actor, UUID target) {
        PartyState state = partyOf(actor).orElse(null);
        if (state == null) return Result.fail("You are not in a party");
        if (actor.equals(target)) return Result.fail("You cannot invite yourself");
        if (membership.containsKey(target)) return Result.fail("That player is already in a party");
        if (state.locked() && !state.owner().equals(actor)) {
            return Result.fail("Only the party owner can invite while the party is locked");
        }
        if (settings.maxSize() > 0 && state.members().size() >= settings.maxSize()) {
            return Result.fail("That party is full");
        }
        pendingMemberInvites.put(target, key(state.name()));
        return Result.ok(state);
    }

    public synchronized Result acceptMemberInvite(UUID playerId) {
        String partyKey = pendingMemberInvites.remove(playerId);
        if (partyKey == null) return Result.fail("You do not have a pending party invite");
        PartyState state = parties.get(partyKey);
        if (state == null) return Result.fail("That party no longer exists");
        if (membership.containsKey(playerId)) {
            Result leave = leave(playerId);
            if (!leave.success()) return leave;
            state = parties.get(partyKey);
            if (state == null) return Result.fail("That party no longer exists");
        }
        if (settings.maxSize() > 0 && state.members().size() >= settings.maxSize()) {
            return Result.fail("That party is full");
        }
        HashSet<UUID> members = new HashSet<>(state.members());
        members.add(playerId);
        pendingMemberInvites.remove(playerId);
        return replace(state, state.withMembers(members));
    }

    public synchronized Optional<PartyState> pendingMemberInvite(UUID playerId) {
        String partyKey = pendingMemberInvites.get(playerId);
        return partyKey == null ? Optional.empty() : Optional.ofNullable(parties.get(partyKey));
    }

    public synchronized Result join(UUID playerId, String name, Optional<String> password) {
        if (membership.containsKey(playerId)) {
            return Result.fail("You are already in a party");
        }
        PartyState state = parties.get(key(name));
        if (state == null) {
            return Result.fail("Party not found");
        }
        if (state.locked()) {
            return Result.fail("That party is locked");
        }
        if (state.password().isPresent() && !state.password().equals(password)) {
            return Result.fail("Invalid party password");
        }
        if (settings.maxSize() > 0 && state.members().size() >= settings.maxSize()) {
            return Result.fail("That party is full");
        }
        HashSet<UUID> members = new HashSet<>(state.members());
        members.add(playerId);
        pendingMemberInvites.remove(playerId);
        return replace(state, state.withMembers(members));
    }

    public synchronized Result leave(UUID playerId) {
        PartyState state = partyOf(playerId).orElse(null);
        if (state == null) {
            return Result.fail("You are not in a party");
        }
        if (state.members().size() == 1) {
            disbandInternal(state);
            save();
            return Result.ok(null);
        }
        HashSet<UUID> members = new HashSet<>(state.members());
        members.remove(playerId);
        PartyState next = state.withMembers(members);
        if (state.owner().equals(playerId)) {
            next = next.withOwner(members.stream().sorted().findFirst().orElseThrow());
        }
        return replace(state, next);
    }

    public synchronized Result disband(UUID actor) {
        PartyState state = requireOwner(actor);
        if (state == null) {
            return Result.fail("Only the party owner can do that");
        }
        disbandInternal(state);
        save();
        return Result.ok(null);
    }

    public synchronized Result kick(UUID actor, UUID target) {
        PartyState state = requireOwner(actor);
        if (state == null) {
            return Result.fail("Only the party owner can do that");
        }
        if (actor.equals(target)) {
            return Result.fail("Use /party quit to leave");
        }
        if (!state.members().contains(target)) {
            return Result.fail("That player is not in your party");
        }
        HashSet<UUID> members = new HashSet<>(state.members());
        members.remove(target);
        return replace(state, state.withMembers(members));
    }

    public synchronized Result changeOwner(UUID actor, UUID target) {
        PartyState state = requireOwner(actor);
        if (state == null) {
            return Result.fail("Only the party owner can do that");
        }
        if (!state.members().contains(target)) {
            return Result.fail("New owner must be a party member");
        }
        return replace(state, state.withOwner(target));
    }

    public synchronized Result rename(UUID actor, String newName) {
        PartyState state = requireOwner(actor);
        if (state == null) {
            return Result.fail("Only the party owner can do that");
        }
        String validated = validateName(newName);
        if (state.name().equalsIgnoreCase(validated)) {
            return Result.fail("That is already the name of your party");
        }
        if (parties.containsKey(key(validated))) {
            return Result.fail("A party with that name already exists");
        }
        String oldName = state.name();
        Optional<String> allyName = state.alliance();
        remove(state);
        PartyState next = state.withName(validated);
        index(next);
        allyName.flatMap(this::party).ifPresent(ally -> replaceWithoutSave(
                ally, ally.withAlliance(Optional.of(validated))));
        String oldKey = key(oldName);
        String newKey = key(validated);
        String inviteToRenamedParty = pendingAllianceInvites.remove(oldKey);
        if (inviteToRenamedParty != null) pendingAllianceInvites.put(newKey, inviteToRenamedParty);
        pendingAllianceInvites.replaceAll((target, inviter) ->
                inviter.equals(oldKey) ? newKey : inviter);
        save();
        return Result.ok(next);
    }

    public synchronized Result setPassword(UUID actor, Optional<String> password) {
        return mutateOwner(actor, state -> state.withPassword(password));
    }

    public synchronized Result setLocked(UUID actor, boolean locked) {
        return mutateOwner(actor, state -> state.withLocked(locked));
    }

    public synchronized Result setXpShare(UUID actor, ShareMode mode) {
        if (mode == ShareMode.RANDOM) {
            return Result.fail("XP sharing supports only NONE or EQUAL");
        }
        return mutateOwnerFeature(actor, PartyFeature.XP_SHARE, state -> state.withXpShare(mode));
    }

    public synchronized Result setItemShare(UUID actor, ShareMode mode) {
        return mutateOwnerFeature(actor, PartyFeature.ITEM_SHARE, state -> state.withItemShare(mode));
    }

    public synchronized Result setItemShareCategory(
            UUID actor, ItemShareCategory category, boolean enabled) {
        return mutateOwnerFeature(actor, PartyFeature.ITEM_SHARE,
                state -> state.withItemShareCategory(category, enabled));
    }

    public synchronized Result inviteAlliance(UUID actor, String targetPartyName) {
        PartyState source = requireOwner(actor);
        if (source == null) {
            return Result.fail("Only the party owner can do that");
        }
        if (!featureUnlocked(source, PartyFeature.ALLIANCE)) {
            return Result.fail("Party alliances unlock at party level "
                    + settings.unlockLevel(PartyFeature.ALLIANCE));
        }
        PartyState target = parties.get(key(targetPartyName));
        if (target == null) {
            return Result.fail("Party not found");
        }
        if (source.name().equalsIgnoreCase(target.name())) {
            return Result.fail("A party cannot ally with itself");
        }
        if (source.alliance().isPresent() || target.alliance().isPresent()) {
            return Result.fail("One of the parties already has an ally");
        }
        if (!featureUnlocked(target, PartyFeature.ALLIANCE)) {
            return Result.fail("The target party has not unlocked alliances");
        }
        pendingAllianceInvites.put(key(target.name()), key(source.name()));
        return Result.ok(source);
    }

    public synchronized Result acceptAlliance(UUID actor) {
        PartyState target = requireOwner(actor);
        if (target == null) {
            return Result.fail("Only the party owner can do that");
        }
        String sourceKey = pendingAllianceInvites.remove(key(target.name()));
        PartyState source = sourceKey == null ? null : parties.get(sourceKey);
        if (source == null) {
            return Result.fail("No pending party alliance invite");
        }
        if (source.alliance().isPresent() || target.alliance().isPresent()) {
            return Result.fail("One of the parties already has an ally");
        }
        replaceWithoutSave(source, source.withAlliance(Optional.of(target.name())));
        PartyState updatedTarget = target.withAlliance(Optional.of(source.name()));
        replaceWithoutSave(target, updatedTarget);
        save();
        return Result.ok(updatedTarget);
    }

    public synchronized Result disbandAlliance(UUID actor) {
        PartyState source = requireOwner(actor);
        if (source == null) {
            return Result.fail("Only the party owner can do that");
        }
        Optional<String> allyName = source.alliance();
        if (allyName.isEmpty()) {
            return Result.fail("Your party does not have an ally");
        }
        PartyState ally = parties.get(key(allyName.orElseThrow()));
        replaceWithoutSave(source, source.withAlliance(Optional.empty()));
        if (ally != null) {
            replaceWithoutSave(ally, ally.withAlliance(Optional.empty()));
        }
        save();
        return Result.ok(source.withAlliance(Optional.empty()));
    }

    public synchronized PartyXpResult applyXpGain(
            UUID playerId,
            double xp,
            int onlineMemberCount,
            int nearMemberCount) {
        if (!Double.isFinite(xp) || xp <= 0.0D) {
            return PartyXpResult.noChange(partyOf(playerId).orElse(null));
        }
        PartyState party = partyOf(playerId).orElse(null);
        if (party == null || party.level() >= settings.levelCap()) {
            return PartyXpResult.noChange(party);
        }
        if (settings.nearMembersNeeded() && nearMemberCount <= 0) {
            return PartyXpResult.noChange(party);
        }
        int previousLevel = party.level();
        int level = previousLevel;
        double accumulated = party.xp() + xp;
        while (level < settings.levelCap()) {
            PartyState calculationState = party.withProgress(level, accumulated);
            int required = xpToNextLevel(calculationState, onlineMemberCount);
            if (accumulated < required) {
                break;
            }
            accumulated -= required;
            level++;
        }
        if (level >= settings.levelCap() && accumulated >= xpToNextLevel(
                party.withProgress(Math.min(level, settings.levelCap()), accumulated),
                onlineMemberCount)) {
            accumulated = 0.0D;
        }
        PartyState updated = party.withProgress(level, accumulated);
        replaceWithoutSave(party, updated);
        save();
        return new PartyXpResult(updated, previousLevel, level);
    }

    private Result mutateOwner(UUID actor, java.util.function.UnaryOperator<PartyState> update) {
        PartyState state = requireOwner(actor);
        if (state == null) {
            return Result.fail("Only the party owner can do that");
        }
        return replace(state, update.apply(state));
    }

    private Result mutateOwnerFeature(
            UUID actor,
            PartyFeature feature,
            java.util.function.UnaryOperator<PartyState> update) {
        PartyState state = requireOwner(actor);
        if (state == null) {
            return Result.fail("Only the party owner can do that");
        }
        if (!featureUnlocked(state, feature)) {
            return Result.fail(feature + " unlocks at party level " + settings.unlockLevel(feature));
        }
        return replace(state, update.apply(state));
    }

    private PartyState requireOwner(UUID actor) {
        PartyState state = partyOf(actor).orElse(null);
        return state != null && state.owner().equals(actor) ? state : null;
    }

    private Result replace(PartyState oldState, PartyState next) {
        replaceWithoutSave(oldState, next);
        save();
        return Result.ok(next);
    }

    private void replaceWithoutSave(PartyState oldState, PartyState next) {
        remove(oldState);
        index(next);
    }


    public synchronized CleanupResult removeInactiveMembers(
            Set<UUID> onlinePlayers,
            java.util.function.Function<UUID, java.time.Instant> lastSeen,
            java.time.Instant cutoff) {
        Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        Objects.requireNonNull(lastSeen, "lastSeen");
        Objects.requireNonNull(cutoff, "cutoff");
        int removed = 0;
        for (PartyState party : java.util.List.copyOf(parties.values())) {
            for (UUID member : java.util.List.copyOf(party.members())) {
                if (onlinePlayers.contains(member)) continue;
                java.time.Instant seen = lastSeen.apply(member);
                if (!seen.equals(java.time.Instant.EPOCH) && seen.isBefore(cutoff)) {
                    Result result = leave(member);
                    if (result.success()) removed++;
                }
            }
        }
        return new CleanupResult(removed);
    }

    private void index(PartyState state) {
        parties.put(key(state.name()), state);
        state.members().forEach(id -> membership.put(id, key(state.name())));
    }

    private void remove(PartyState state) {
        parties.remove(key(state.name()));
        state.members().forEach(membership::remove);
    }

    private void disbandInternal(PartyState state) {
        state.alliance().flatMap(this::party).ifPresent(ally -> replaceWithoutSave(
                ally, ally.withAlliance(Optional.empty())));
        remove(state);
        pendingAllianceInvites.remove(key(state.name()));
        pendingAllianceInvites.values().removeIf(value -> value.equals(key(state.name())));
    }

    private void repairAlliances() {
        boolean changed = false;
        for (PartyState party : java.util.List.copyOf(parties.values())) {
            if (party.alliance().isEmpty()) {
                continue;
            }
            PartyState ally = parties.get(key(party.alliance().orElseThrow()));
            if (ally == null || ally.alliance().isEmpty()
                    || !ally.alliance().orElseThrow().equalsIgnoreCase(party.name())) {
                replaceWithoutSave(party, party.withAlliance(Optional.empty()));
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    private void save() {
        try {
            store.save(Map.copyOf(parties));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String key(String name) {
        return Objects.requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
    }

    private static String validateName(String name) {
        String value = Objects.requireNonNull(name, "name").trim();
        if (!value.matches("[A-Za-z0-9_]{1,32}")) {
            throw new IllegalArgumentException(
                    "Party names must be 1-32 letters, numbers, or underscores");
        }
        return value;
    }

    public record Result(boolean success, String detail, Optional<PartyState> party) {
        public Result {
            Objects.requireNonNull(detail, "detail");
            Objects.requireNonNull(party, "party");
        }

        static Result ok(PartyState party) {
            return new Result(true, "", Optional.ofNullable(party));
        }

        static Result fail(String detail) {
            return new Result(false, detail, Optional.empty());
        }
    }

    public record CleanupResult(int removedMembers) { }

    public record PartyXpResult(
            Optional<PartyState> party,
            int oldLevel,
            int newLevel) {
        public PartyXpResult {
            Objects.requireNonNull(party, "party");
        }

        PartyXpResult(PartyState party, int oldLevel, int newLevel) {
            this(Optional.ofNullable(party), oldLevel, newLevel);
        }

        public static PartyXpResult noChange(PartyState party) {
            int level = party == null ? 0 : party.level();
            return new PartyXpResult(party, level, level);
        }

        public int levelsGained() {
            return newLevel - oldLevel;
        }
    }
}
