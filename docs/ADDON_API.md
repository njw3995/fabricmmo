# FabricMMO Addon and Datapack API

FabricMMO API 1.4 is designed so external Fabric mods can register skills and content without importing `fabricmmo-core` implementation packages or patching the base mod. Core remains authoritative for progression, permissions, placed-block tracking, protection, abilities, drops, replanting, persistence, and UI state.

## Registration lifecycle

Declare a `fabricmmo` Fabric Loader entrypoint and implement `FabricMmoEntrypoint`:

```json
{
  "entrypoints": {
    "fabricmmo": ["com.example.ExampleFabricMmoEntrypoint"]
  }
}
```

```java
public final class ExampleFabricMmoEntrypoint implements FabricMmoEntrypoint {
    @Override
    public void register(FabricMmoApi api) {
        ApiVersion.requireAtLeast(api.apiVersion(), 1, 4, "Example addon");
        ExampleSkills.register(api);
        ExampleContent.register(api);
    }
}
```

Skill, XP-source, ability, configuration, content, command, UI, and protection-provider registrations must occur in this callback. Core freezes Java registries immediately afterward and rejects late or duplicate registrations.

The addon owns subscriptions to the source mod or Fabric callback. Store every returned subscription or listener handle and unregister it during server shutdown. FabricMMO clears its own per-player ability state on disconnect, but it cannot unregister another mod's event listener on the addon's behalf.

## Skills, arbitrary listeners, and event-driven XP

Use namespaced `SkillDefinition` and `XpSourceDefinition` values. The addon listens directly to the relevant Fabric event, Minecraft callback, Mixin-owned callback, or source mod's public server event. FabricMMO does not need to know that event type.

For normal gameplay, submit the verified outcome through `GameplayXpService.awardOnline(XpAwardRequest)`:

```java
sourceModEvents.machineCompleted(event -> {
    if (!event.serverVerified()) return;
    api.gameplayXp().awardOnline(new XpAwardRequest(
            event.playerId(),
            NamespacedId.parse("example:engineering"),
            NamespacedId.parse("example:machine_completed"),
            event.xp(),
            Map.of("machine", event.machineId().toString())));
});
```

The online gateway validates registration, player presence, Creative/Spectator state, skill and source permissions, XP perks, dynamic permission-aware Power Level membership, XP events, rates, caps, persistence, scoreboards, and XP bars. `ProgressionService.award` remains the lower-level mutation pipeline for controlled administrative, migration, and isolated-test use; gameplay listeners should prefer `gameplayXp()`.

`FabricMmoEventBus` is type-generic. Addons may publish and subscribe to their own event classes for cross-addon coordination, but they usually listen to the source mod's own event bus directly. FabricMMO intentionally does not duplicate every possible Minecraft or third-party event.

This is the normal extension boundary:

| Responsibility | Owner |
|---|---|
| Detect a source-mod outcome | External addon listener |
| Validate source-specific ownership and payload | External addon |
| Validate online player, game mode, permissions, XP source, perks, rates, caps, and Power Level | FabricMMO `gameplayXp()` |
| Persist progression and update events/UI | FabricMMO core |
| Apply source-specific gameplay effects | External addon |

Therefore, adding a new battle, machine, quest, capture, spell, farming, or technology skill does not require a FabricMMO event class. The external addon defines or consumes the event and submits the result.

Datapacks intentionally do not create event-driven skills. A datapack cannot safely decide that a battle, capture, machine operation, or another mod-specific outcome occurred. Those integrations require a small Fabric addon listener, but they do not require a FabricMMO core change.

When the source mod exposes no event, the integration addon may use a narrowly scoped Mixin in its own JAR to observe a stable server-side completion method. That is still an addon implementation detail and does not require patching FabricMMO. If the source mod never produces the gameplay outcome at all, no observer can award completion XP until another mod supplies that outcome.


## Addon configuration defaults

Register configuration during the FabricMMO entrypoint callback. After server startup, core materializes it under `config/fabricmmo/addons/<namespace>/<owner-path>/<fileName>`, preserves existing administrator values, appends newly introduced defaults, and exposes raw or typed reads:

```java
NamespacedId owner = NamespacedId.parse("example:machines");
api.configRegistrar().registerConfig(new ConfigContribution(
        owner,
        "settings.properties",
        Map.of("Enabled", "true", "XpMultiplier", "1.0")));

boolean enabled = api.config().booleanValue(owner, "settings.properties", "Enabled", true);
double multiplier = api.config().doubleValue(
        owner, "settings.properties", "XpMultiplier", 1.0D);
```

Malformed typed values throw descriptive errors and are not silently replaced. `config().reload()` refreshes addon files when the addon chooses to expose a reload action.

## Gathering content

Java addons may register a block ID or block tag with Mining, Woodcutting, or Herbalism:

```java
api.gatheringContentRegistrar().registerGatheringContent(
        GatheringContentDefinition.builder(
                        NamespacedId.parse("example:crystal_ores"),
                        NamespacedId.parse("fabricmmo:mining"),
                        ContentSelector.tag("example:crystal_ores"),
                        125)
                .validTools(ContentSelector.tag("minecraft:pickaxes"))
                .bonusDrops(true)
                .activeAbility(true)
                .build());
```

Core processes registered content through the existing skill handlers. `natural_blocks_only` uses FabricMMO's placed-block tracker. Protection checks, fake-player exclusions, tools, XP rates, Super Breaker, Blast Mining, Tree Feller, Green Terra, Green Thumb, and bonus drops remain core-owned.

## Brewing content

External recipes remain owned and executed by the source mod. A brewing declaration identifies a completed ingredient/input/output transformation and the corresponding Alchemy stage. FabricMMO observes the server-side brewing stand and awards the normal configured stage XP to its persisted owner.

```java
api.brewingContentRegistrar().registerBrewingContent(
        BrewingContentDefinition.builder(
                        NamespacedId.parse("example:healing_brew"),
                        ContentSelector.id("example:herb"),
                        ContentSelector.id("example:unfinished_brew"),
                        ContentSelector.id("example:healing_brew"),
                        2)
                .build());
```

## Entity XP content

Static entity IDs or tags can participate in core Combat or Taming XP without adding a dependency to core:

```java
api.entityXpContentRegistrar().registerEntityXpContent(
        EntityXpContentDefinition.of(
                NamespacedId.parse("example:crystal_golem_combat"),
                EntityXpContentDefinition.Scope.COMBAT,
                ContentSelector.id("example:crystal_golem"),
                2.5));
```

For `COMBAT`, `xp` is the base amount multiplied by actual health damage and the existing origin/exploit multiplier. For `TAMING`, it is the one-time value awarded by the normal server-side tame event. A value of zero explicitly disables XP for matching content.

## Datapack overlays

Static content can be added or overridden without Java under:

```text
data/<namespace>/fabricmmo/gathering/*.json
data/<namespace>/fabricmmo/entity_xp/*.json
data/<namespace>/fabricmmo/brewing/*.json
```

Definitions use `format: 1`. The default ID is derived from the namespace and path, or an explicit namespaced `id` can be supplied. A datapack definition overrides a Java registration with the same ID. `{ "format": 1, "enabled": false }` disables that ID. Invalid files are reported and skipped without replacing the last valid Java registration for unrelated IDs. Successful `/reload` refreshes definitions and tag-resolution caches.

A complete Minecraft 1.21.1 example pack is in `examples/fabricmmo-content-datapack`.

### Gathering schema

```json
{
  "format": 1,
  "id": "example:berry_crop",
  "skill": "fabricmmo:herbalism",
  "block": "#example:berry_crops",
  "xp": 40,
  "valid_tools": ["#minecraft:hoes"],
  "natural_blocks_only": false,
  "maturity": {"mode": "maximum", "property": "age"},
  "bonus_drops": true,
  "active_ability": true,
  "replant": {
    "planting_item": "example:berry_seed",
    "age_property": "age",
    "rank_ages": [0, 1, 2, 3, 4],
    "active_ability_rank_bonus": 1,
    "delay_ticks": 2
  },
  "metadata": {"source_mod": "example"}
}
```

`maturity.mode` supports `any`, `maximum`, and `at_least`. `at_least` also requires an integer `value`.

### Entity XP schema

```json
{
  "format": 1,
  "scope": "combat",
  "entity": "#example:crystal_golems",
  "xp": 2.5,
  "metadata": {"source_mod": "example"}
}
```

### Brewing schema

```json
{
  "format": 1,
  "ingredient": "example:herb",
  "input": "example:unfinished_brew",
  "output": "example:healing_brew",
  "stage": 2
}
```

Selectors are either `namespace:id` or `#namespace:tag`.

## Persistent addon markers

`PersistentMarkerService` stores small namespaced, world-scoped strings for first-time milestones and replay rejection. Core preserves the data while an addon is absent, saves it asynchronously, writes atomically, retries failures, and performs a final synchronous shutdown flush.

The service is not world-bound during registration. Use it from `SERVER_STARTING` or later after checking `available()`:

```java
if (api.persistentMarkers().available()) {
    boolean first = api.persistentMarkers().markOnce(
            NamespacedId.parse("example:first_discovery"),
            player.getUuidAsString(),
            discoveredId.toString());
}
```

Values are limited to small stable identifiers, not arbitrary documents. Data is stored in `world/data/fabricmmo/addon-markers.properties`.

## Active abilities and passives

An addon can let core own preparation, activation, duration, cooldowns, and normal ability-state events:

```java
if (api.abilities().prepareOnline(playerId, ABILITY_ID)) {
    // Wait for the addon-defined activation action.
}
if (api.abilities().activateOnline(playerId, ABILITY_ID)) {
    applyAddonGameplayEffect();
}
```

The online methods resolve the registered skill level and revalidate online presence, Creative/Spectator state, skill permission, and ability permission. The low-level `prepare(..., skillLevel)` and `activate(...)` methods are intended for controlled tests, migration, or non-player contexts. The addon still listens to and applies its own gameplay effect; FabricMMO owns the registered timing state. Addons with a specialized timer may instead publish a delegated view through `abilityStateRegistrar()`.

Registered passives can use FabricMMO's unlock check, cancellable probability event, and deterministic RNG abstraction:

```java
boolean active = api.passives().rollOnline(
        playerId, PASSIVE_ID, baseProbability, randomSource).activated();
```

The online resolver validates player eligibility, skill/passive permissions, unlock level, the cancellable probability event, and the supplied RNG. The low-level `roll(...)` method is reserved for controlled non-gameplay contexts. FabricMMO exposes active/cooldown state to scoreboards, XP bars, commands, and debug readers regardless of which state model is used.

## Protection providers

Claims integrations can be separate compatibility addons:

```java
api.protectionProviderRegistrar().registerProtectionProvider(
        NamespacedId.parse("example:claims"),
        100,
        protectionService);
```

Providers are evaluated deterministically by descending priority and ID. Every provider must allow an action. Exceptions fail closed and deny the action. The registry freezes after addon registration.

## Compatibility rules

- Depend on `fabricmmo-api`; do not import `io.github.njw3995.fabricmmo.core`.
- Do not mutate progression files or databases directly.
- Do not rely on client packets for authoritative outcomes.
- Use stable namespaced IDs and stable replay tokens.
- Keep expensive I/O off the server thread.
- Prefer source-mod registries and tags over hard-coded lists.
- Unsubscribe source-mod listeners on shutdown and clear addon-owned session state on disconnect.
- Check the API version explicitly and fail with a descriptive message when a required capability is missing.
