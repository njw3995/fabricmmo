package io.github.njw3995.fabricmmo.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.event.XpPreAwardEvent;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.bootstrap.FabricMmoBootstrap;
import io.github.njw3995.fabricmmo.core.persistence.InMemoryProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.StoredSkillProgress;
import io.github.njw3995.fabricmmo.core.protection.AllowAllProtectionService;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultProgressionServiceTest {
    @Test
    void awardsValidatedXpAndEmitsLevelEvent() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        UUID player = UUID.randomUUID();
        AtomicInteger levelEvents = new AtomicInteger();
        api.events().subscribe(LevelChangedEvent.class, ignored -> levelEvents.incrementAndGet());
        XpAwardResult result = api.progression().award(new XpAwardRequest(
                player,
                CoreSkills.MINING,
                CoreXpSources.MINING_BLOCK_BREAK,
                1020,
                Map.of()));
        assertEquals(XpAwardResult.Status.APPLIED, result.status());
        assertEquals(1, result.newLevel());
        assertEquals(1, levelEvents.get());
    }

    @Test
    void rejectsUnregisteredXpSources() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        XpAwardResult result = api.progression().award(new XpAwardRequest(
                UUID.randomUUID(),
                CoreSkills.MINING,
                NamespacedId.parse("unknown:source"),
                100,
                Map.of()));
        assertEquals(XpAwardResult.Status.REJECTED, result.status());
    }
    @Test
    void configuredSkillCapStopsProgressionAndClearsOverflow() {
        InMemoryProgressionStore store = new InMemoryProgressionStore();
        ProgressionSettings settings = settings(Map.of(CoreSkills.MINING, 1), Integer.MAX_VALUE, 1.0D);
        var api = FabricMmoBootstrap.create(
                store, allowAll(), Clock.systemUTC(), settings, ignored -> { });
        UUID player = UUID.randomUUID();

        XpAwardResult first = api.progression().award(new XpAwardRequest(
                player, CoreSkills.MINING, CoreXpSources.MINING_BLOCK_BREAK, 5000, Map.of()));
        XpAwardResult second = api.progression().award(new XpAwardRequest(
                player, CoreSkills.MINING, CoreXpSources.MINING_BLOCK_BREAK, 100, Map.of()));

        assertEquals(1, first.newLevel());
        assertEquals(0, api.progression().query(player, CoreSkills.MINING).xp());
        assertEquals(XpAwardResult.Status.REJECTED, second.status());
    }

    @Test
    void normalXpUsesConfiguredMultipliersWhileCommandXpBypassesThem() {
        ProgressionSettings settings = settings(Map.of(), Integer.MAX_VALUE, 2.0D);
        UUID normalPlayer = UUID.randomUUID();
        UUID commandPlayer = UUID.randomUUID();
        var api = FabricMmoBootstrap.create(
                new InMemoryProgressionStore(), allowAll(), Clock.systemUTC(), settings,
                ignored -> { });

        api.progression().award(new XpAwardRequest(
                normalPlayer, CoreSkills.MINING, CoreXpSources.MINING_BLOCK_BREAK, 500, Map.of()));
        api.progression().award(new XpAwardRequest(
                commandPlayer, CoreSkills.MINING,
                CoreXpSources.commandSourceId(CoreSkills.MINING), 1020, Map.of()));

        assertEquals(1, api.progression().query(normalPlayer, CoreSkills.MINING).level());
        assertEquals(31, api.progression().query(normalPlayer, CoreSkills.MINING).xp());
        assertEquals(1, api.progression().query(commandPlayer, CoreSkills.MINING).level());
        assertEquals(0, api.progression().query(commandPlayer, CoreSkills.MINING).xp());
    }

    @Test
    void levelAdministrationClearsXpAndPublishesLevelChanges() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        UUID player = UUID.randomUUID();
        AtomicInteger levelEvents = new AtomicInteger();
        api.events().subscribe(LevelChangedEvent.class, ignored -> levelEvents.incrementAndGet());

        api.progression().award(new XpAwardRequest(
                player, CoreSkills.MINING, CoreXpSources.MINING_BLOCK_BREAK, 500, Map.of()));
        api.progression().setLevel(player, CoreSkills.MINING, 12);
        api.progression().addLevels(player, CoreSkills.MINING, -5);

        assertEquals(7, api.progression().query(player, CoreSkills.MINING).level());
        assertEquals(0, api.progression().query(player, CoreSkills.MINING).xp());
        assertEquals(2, levelEvents.get());
    }

    @Test
    void childSkillAddedLevelsSplitAcrossParents() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        UUID player = UUID.randomUUID();

        api.progression().addLevels(player, CoreSkills.SMELTING, 10);

        assertEquals(5, api.progression().query(player, CoreSkills.MINING).level());
        assertEquals(5, api.progression().query(player, CoreSkills.REPAIR).level());
        assertEquals(5, api.progression().query(player, CoreSkills.SMELTING).level());
        assertEquals(0, api.progression().query(player, CoreSkills.SMELTING).xp());
        assertEquals(5, api.progression().queryAll(player).get(CoreSkills.SMELTING).level());
    }

    @Test
    void directChildXpSplitsEvenlyAcrossParentsAndFiresChildPreEvent() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        UUID player = UUID.randomUUID();
        AtomicInteger childPreEvents = new AtomicInteger();
        AtomicInteger parentPreEvents = new AtomicInteger();
        api.events().subscribe(XpPreAwardEvent.class, event -> {
            if (event.request().skillId().equals(CoreSkills.SMELTING)) {
                childPreEvents.incrementAndGet();
                event.multiplier(2.0D);
            } else if (event.request().skillId().equals(CoreSkills.MINING)
                    || event.request().skillId().equals(CoreSkills.REPAIR)) {
                parentPreEvents.incrementAndGet();
            }
        });

        XpAwardResult result = api.progression().award(new XpAwardRequest(
                player,
                CoreSkills.SMELTING,
                CoreXpSources.SMELTING_FURNACE,
                500,
                Map.of()));

        assertEquals(XpAwardResult.Status.APPLIED, result.status());
        assertEquals(1, childPreEvents.get());
        assertEquals(2, parentPreEvents.get());
        assertEquals(551, api.progression().query(player, CoreSkills.MINING).xp());
        assertEquals(551, api.progression().query(player, CoreSkills.REPAIR).xp());
        assertEquals(0, api.progression().query(player, CoreSkills.SMELTING).xp());
    }

    @Test
    void childXpCancellationDoesNotMutateParents() {
        var api = FabricMmoBootstrap.create(new InMemoryProgressionStore(), ignored -> { });
        UUID player = UUID.randomUUID();
        api.events().subscribe(XpPreAwardEvent.class, event -> {
            if (event.request().skillId().equals(CoreSkills.SMELTING)) {
                event.cancel();
            }
        });

        XpAwardResult result = api.progression().award(new XpAwardRequest(
                player,
                CoreSkills.SMELTING,
                CoreXpSources.SMELTING_FURNACE,
                500,
                Map.of()));

        assertEquals(XpAwardResult.Status.CANCELLED, result.status());
        assertEquals(0, api.progression().query(player, CoreSkills.MINING).xp());
        assertEquals(0, api.progression().query(player, CoreSkills.REPAIR).xp());
    }

    @Test
    void childSkillLevelIsAverageOfCappedParents() {
        InMemoryProgressionStore store = new InMemoryProgressionStore();
        UUID player = UUID.randomUUID();
        store.save(new PlayerProgressionData(player, 1, Map.of(
                CoreSkills.MINING, new StoredSkillProgress(200, 0),
                CoreSkills.REPAIR, new StoredSkillProgress(40, 0))));
        ProgressionSettings settings = settings(
                Map.of(CoreSkills.MINING, 100, CoreSkills.REPAIR, 100),
                Integer.MAX_VALUE,
                1.0D);
        var api = FabricMmoBootstrap.create(
                store, allowAll(), Clock.systemUTC(), settings, ignored -> { });

        assertEquals(70, api.progression().query(player, CoreSkills.SMELTING).level());
    }

    @Test
    void powerCapUsesPermissionAwarePowerAfterFastUpperBoundCheck() {
        InMemoryProgressionStore store = new InMemoryProgressionStore();
        UUID player = UUID.randomUUID();
        store.save(new PlayerProgressionData(player, 1, Map.of(
                CoreSkills.MINING, new StoredSkillProgress(1, 0),
                CoreSkills.WOODCUTTING, new StoredSkillProgress(1, 0))));
        ProgressionSettings settings = settings(Map.of(), 2, 1.0D);
        var api = FabricMmoBootstrap.create(
                store, allowAll(), Clock.systemUTC(), settings, ignored -> { });

        XpAwardResult permittedSubset = api.progression().award(new XpAwardRequest(
                player, CoreSkills.MINING, CoreXpSources.MINING_BLOCK_BREAK, 1040,
                Map.of("powerLevelSkills", CoreSkills.MINING.toString())));

        assertEquals(XpAwardResult.Status.APPLIED, permittedSubset.status());
        assertEquals(2, permittedSubset.newLevel());
    }

    private static ProgressionSettings settings(
            Map<NamespacedId, Integer> capOverrides,
            int powerLevelCap,
            double miningMultiplier) {
        ProgressionSettings defaults = ProgressionSettings.upstreamDefaults();
        Map<NamespacedId, Integer> caps = new HashMap<>(defaults.levelCaps());
        caps.putAll(capOverrides);
        Map<NamespacedId, Double> multipliers = new HashMap<>(defaults.skillXpMultipliers());
        multipliers.put(CoreSkills.MINING, miningMultiplier);
        return new ProgressionSettings(
                defaults.mode(),
                defaults.formulaType(),
                defaults.curve(),
                defaults.cumulativeCurve(),
                defaults.globalXpMultiplier(),
                multipliers,
                caps,
                powerLevelCap,
                defaults.truncateSkills(),
                defaults.earlyGameBoostEnabled(),
                defaults.customXpPerkBoost(),
                defaults.diminishedReturnsEnabled(),
                defaults.diminishedReturnsMinimumFraction(),
                defaults.diminishedReturnsThresholds(),
                Duration.ofMinutes(10));
    }

    private static AllowAllProtectionService allowAll() {
        return new AllowAllProtectionService();
    }

}
