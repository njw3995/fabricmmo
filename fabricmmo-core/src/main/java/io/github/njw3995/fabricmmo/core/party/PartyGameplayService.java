package io.github.njw3995.fabricmmo.core.party;

import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.player.PlayerVisibilityService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Live party XP and item-sharing behavior translated from the pinned upstream implementation. */
public final class PartyGameplayService {
    public static final String SHARED_CONTEXT = "fabricmmo.party.shared";
    private final MinecraftServer server;
    private final PartyService parties;
    private final PartySettings settings;
    private final ItemWeightSettings itemWeights;
    private final ItemShareClassifier classifier;
    private final PlayerVisibilityService visibility;
    private final PartyShareAllocator allocator;

    public PartyGameplayService(
            MinecraftServer server,
            PartyService parties,
            ItemWeightSettings itemWeights,
            PlayerVisibilityService visibility,
            Random random) {
        this.server = Objects.requireNonNull(server, "server");
        this.parties = Objects.requireNonNull(parties, "parties");
        this.settings = parties.settings();
        this.itemWeights = Objects.requireNonNull(itemWeights, "itemWeights");
        this.classifier = new ItemShareClassifier(itemWeights);
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        this.allocator = new PartyShareAllocator(random);
    }

    public Optional<XpAwardResult> distributeXp(
            XpAwardRequest request,
            Function<XpAwardRequest, XpAwardResult> awarder) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(awarder, "awarder");
        if ("true".equals(request.context().get(SHARED_CONTEXT))) {
            return Optional.empty();
        }
        ServerPlayerEntity source = server.getPlayerManager().getPlayer(request.playerId());
        if (source == null) {
            return Optional.empty();
        }
        PartyState party = parties.partyOf(source.getUuid()).orElse(null);
        if (party == null || party.xpShare() != ShareMode.EQUAL
                || !parties.featureUnlocked(party, PartyFeature.XP_SHARE)) {
            return Optional.empty();
        }
        List<ServerPlayerEntity> eligible = nearMembers(source, party, true);
        if (eligible.size() <= 1) {
            return Optional.empty();
        }
        double bonus = Math.min(
                settings.xpShareBonusBase() + eligible.size() * settings.xpShareBonusIncrease(),
                settings.xpShareBonusCap());
        double splitXp = request.rawXp() / eligible.size() * bonus;
        XpAwardResult sourceResult = null;
        for (ServerPlayerEntity member : eligible) {
            Map<String, String> context = new HashMap<>(request.context());
            context.put(SHARED_CONTEXT, "true");
            context.put("fabricmmo.party.source", request.playerId().toString());
            XpAwardResult result = awarder.apply(new XpAwardRequest(
                    member.getUuid(), request.skillId(), request.sourceId(), splitXp, context));
            if (member.getUuid().equals(source.getUuid())) {
                sourceResult = result;
            }
        }
        if (sourceResult == null) {
            sourceResult = new XpAwardResult(
                    XpAwardResult.Status.REJECTED, 0, 0, 0,
                    "Party XP sharing did not award the initiating player");
        }
        return Optional.of(sourceResult);
    }

    public PartyService.PartyXpResult recordPartyXp(UUID playerId, double appliedXp) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player == null) {
            return PartyService.PartyXpResult.noChange(parties.partyOf(playerId).orElse(null));
        }
        PartyState party = parties.partyOf(playerId).orElse(null);
        if (party == null) {
            return PartyService.PartyXpResult.noChange(null);
        }
        int online = 0;
        int near = 0;
        for (UUID memberId : party.members()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
            if (member == null) {
                continue;
            }
            online++;
            if (!memberId.equals(playerId) && sameWorldAndRange(player, member)) {
                near++;
            }
        }
        return parties.applyXpGain(playerId, appliedXp, online, near);
    }

    public boolean shareItem(ServerPlayerEntity picker, ItemEntity entity) {
        Objects.requireNonNull(picker, "picker");
        Objects.requireNonNull(entity, "entity");
        PartyState party = parties.partyOf(picker.getUuid()).orElse(null);
        if (party == null || party.itemShare() == ShareMode.NONE
                || !parties.featureUnlocked(party, PartyFeature.ITEM_SHARE)) {
            return false;
        }
        ItemStack stack = entity.getStack();
        if (stack.isEmpty()) {
            return false;
        }
        String itemPath = Registries.ITEM.getId(stack.getItem()).getPath();
        ItemShareCategory category = classifier.classify(itemPath).orElse(null);
        if (category == null || !party.itemShareCategories().contains(category)) {
            return false;
        }
        List<ServerPlayerEntity> eligible = nearMembers(picker, party, false);
        if (eligible.size() <= 1) {
            return false;
        }
        List<UUID> ids = eligible.stream().map(ServerPlayerEntity::getUuid).toList();
        List<UUID> winners = allocator.allocate(
                party.itemShare(), ids, stack.getCount(), itemWeights.weight(itemPath));
        if (winners.isEmpty()) {
            return false;
        }
        Map<UUID, ServerPlayerEntity> byId = new HashMap<>();
        eligible.forEach(player -> byId.put(player.getUuid(), player));
        entity.discard();
        ItemStack one = stack.copyWithCount(1);
        for (UUID winnerId : winners) {
            ServerPlayerEntity winner = byId.get(winnerId);
            if (winner == null) {
                continue;
            }
            ItemStack awarded = one.copy();
            if (!winner.getInventory().insertStack(awarded)) {
                winner.dropItem(awarded, false);
            }
        }
        return true;
    }

    public void remove(UUID playerId) {
        allocator.remove(playerId);
    }

    public void clear() {
        allocator.clear();
    }

    private List<ServerPlayerEntity> nearMembers(
            ServerPlayerEntity source,
            PartyState party,
            boolean requireVisible) {
        ArrayList<ServerPlayerEntity> result = new ArrayList<>();
        for (UUID memberId : party.members()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
            if (member == null || !sameWorldAndRange(source, member)) {
                continue;
            }
            if (requireVisible && !visibility.visibleTo(member, source)) {
                continue;
            }
            result.add(member);
        }
        if (result.stream().noneMatch(player -> player.getUuid().equals(source.getUuid()))) {
            result.add(source);
        }
        return List.copyOf(result);
    }

    private boolean sameWorldAndRange(ServerPlayerEntity first, ServerPlayerEntity second) {
        return first.getServerWorld() == second.getServerWorld()
                && first.squaredDistanceTo(second) <= settings.shareRange() * settings.shareRange();
    }
}
