package io.github.njw3995.fabricmmo.core.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.BrewingContentDefinition;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentDefinition;
import io.github.njw3995.fabricmmo.api.content.GatheringContentDefinition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads generic addon content from server datapacks on startup and /reload. */
public final class DatapackContentLoader {
    public static final String GATHERING_PATH = "fabricmmo/gathering";
    public static final String ENTITY_XP_PATH = "fabricmmo/entity_xp";
    public static final String BREWING_PATH = "fabricmmo/brewing";
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/Datapacks");

    private DatapackContentLoader() {}

    public static Report reload(
            ResourceManager resources,
            DefaultGatheringContentRegistry gathering,
            DefaultEntityXpContentRegistry entityXp,
            DefaultBrewingContentRegistry brewing) {
        Loaded<GatheringContentDefinition> gatheringLoaded = load(
                resources,
                GATHERING_PATH,
                (id, resource) -> DatapackContentParser.gathering(id, resource.getReader()),
                GatheringContentDefinition::id);
        Loaded<EntityXpContentDefinition> entityXpLoaded = load(
                resources,
                ENTITY_XP_PATH,
                (id, resource) -> DatapackContentParser.entityXp(id, resource.getReader()),
                EntityXpContentDefinition::id);
        Loaded<BrewingContentDefinition> brewingLoaded = load(
                resources,
                BREWING_PATH,
                (id, resource) -> DatapackContentParser.brewing(id, resource.getReader()),
                BrewingContentDefinition::id);

        List<GatheringContentDefinition> acceptedGathering = new ArrayList<>();
        int validationFailures = 0;
        for (GatheringContentDefinition definition : gatheringLoaded.definitions()) {
            String error = gathering.datapackValidationError(definition);
            if (error.isEmpty()) {
                acceptedGathering.add(definition);
            } else {
                validationFailures++;
                LOGGER.error("Invalid FabricMMO gathering integration {}: {}", definition.id(), error);
            }
        }

        gathering.replaceDatapackDefinitions(acceptedGathering, gatheringLoaded.disabled());
        entityXp.replaceDatapackDefinitions(entityXpLoaded.definitions(), entityXpLoaded.disabled());
        brewing.replaceDatapackDefinitions(brewingLoaded.definitions(), brewingLoaded.disabled());
        Report report = new Report(
                acceptedGathering.size(),
                gatheringLoaded.disabled().size(),
                entityXpLoaded.definitions().size(),
                entityXpLoaded.disabled().size(),
                brewingLoaded.definitions().size(),
                brewingLoaded.disabled().size(),
                gatheringLoaded.failures() + entityXpLoaded.failures()
                        + brewingLoaded.failures() + validationFailures);
        LOGGER.info(
                "Loaded {} gathering, {} entity XP, and {} brewing datapack integrations "
                        + "({} disabled, {} invalid)",
                report.gatheringDefinitions(), report.entityXpDefinitions(),
                report.brewingDefinitions(), report.disabledDefinitions(),
                report.invalidResources());
        return report;
    }

    private static <T> Loaded<T> load(
            ResourceManager resources,
            String path,
            Parser<T> parser,
            Function<T, NamespacedId> idExtractor) {
        Map<Identifier, Resource> found = resources.findResources(
                path, id -> id.getPath().endsWith(".json"));
        TreeMap<NamespacedId, T> definitions = new TreeMap<>();
        TreeSet<NamespacedId> disabled = new TreeSet<>();
        Set<NamespacedId> invalidIds = new HashSet<>();
        int failures = 0;
        for (var entry : found.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            try {
                DatapackContentParser.Parsed<T> parsed = parser.parse(entry.getKey(), entry.getValue());
                NamespacedId id = parsed.id();
                if (invalidIds.contains(id) || definitions.containsKey(id) || disabled.contains(id)) {
                    definitions.remove(id);
                    disabled.remove(id);
                    invalidIds.add(id);
                    failures++;
                    LOGGER.error("Duplicate FabricMMO datapack integration id {} in {}", id, entry.getKey());
                    continue;
                }
                if (parsed.definition().isPresent()) {
                    T definition = parsed.definition().orElseThrow();
                    NamespacedId extracted = idExtractor.apply(definition);
                    if (!id.equals(extracted)) {
                        throw new IllegalArgumentException(
                                "Parsed id " + id + " does not match definition id " + extracted);
                    }
                    definitions.put(id, definition);
                } else {
                    disabled.add(id);
                }
            } catch (IOException | RuntimeException exception) {
                failures++;
                LOGGER.error("Invalid FabricMMO datapack integration {}: {}",
                        entry.getKey(), exception.getMessage(), exception);
            }
        }
        return new Loaded<>(List.copyOf(definitions.values()), Set.copyOf(disabled), failures);
    }

    public record Report(
            int gatheringDefinitions,
            int disabledGatheringDefinitions,
            int entityXpDefinitions,
            int disabledEntityXpDefinitions,
            int brewingDefinitions,
            int disabledBrewingDefinitions,
            int invalidResources) {
        public int disabledDefinitions() {
            return disabledGatheringDefinitions + disabledEntityXpDefinitions
                    + disabledBrewingDefinitions;
        }
    }

    private record Loaded<T>(List<T> definitions, Set<NamespacedId> disabled, int failures) {}

    @FunctionalInterface
    private interface Parser<T> {
        DatapackContentParser.Parsed<T> parse(Identifier id, Resource resource) throws IOException;
    }
}
