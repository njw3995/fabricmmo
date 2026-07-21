package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.GameplayXpService;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistryView;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Online gameplay validation shared by all external XP listeners. */
public final class DefaultGameplayXpService implements GameplayXpService {
    private final SkillRegistryView skills;
    private final XpSourceRegistryView sources;
    private final ProgressionService progression;
    private final ProgressionSettings settings;
    private volatile PlayerResolver resolver = ignored -> Optional.empty();

    public DefaultGameplayXpService(
            SkillRegistryView skills,
            XpSourceRegistryView sources,
            ProgressionService progression,
            ProgressionSettings settings) {
        this.skills = Objects.requireNonNull(skills, "skills");
        this.sources = Objects.requireNonNull(sources, "sources");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    DefaultGameplayXpService(
            SkillRegistryView skills,
            XpSourceRegistryView sources,
            ProgressionService progression,
            ProgressionSettings settings,
            PlayerResolver resolver) {
        this(skills, sources, progression, settings);
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public void bind(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        FabricCommandPermissionService permissions = new FabricCommandPermissionService();
        resolver = playerId -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                return Optional.empty();
            }
            if (player.isRemoved()) {
                return Optional.of(PlayerContext.rejected("Player entity is removed"));
            }
            if (player.isCreative()) {
                return Optional.of(PlayerContext.rejected("Creative players cannot earn gameplay XP"));
            }
            if (player.isSpectator()) {
                return Optional.of(PlayerContext.rejected("Spectators cannot earn gameplay XP"));
            }
            Predicate<String> hasPermission = permission -> permissions.hasPermission(
                    player.getCommandSource(), permission, true);
            return Optional.of(PlayerContext.eligible(
                    hasPermission,
                    (base, skillId) -> PlayerProgressionContext.enrich(
                            player, base, settings, skillId, skills)));
        };
    }

    @Override
    public XpAwardResult awardOnline(XpAwardRequest request) {
        Objects.requireNonNull(request, "request");
        SkillDefinition skill = skills.find(request.skillId()).orElse(null);
        if (skill == null) {
            return rejected("Unknown skill " + request.skillId());
        }
        XpSourceDefinition source = sources.find(request.sourceId()).orElse(null);
        if (source == null) {
            return rejected("Unknown XP source " + request.sourceId());
        }
        if (!source.skillId().equals(request.skillId())) {
            return rejected("XP source " + request.sourceId() + " does not target "
                    + request.skillId());
        }

        PlayerContext player = resolver.resolve(request.playerId()).orElse(null);
        if (player == null) {
            return rejected("Player is not online on this server");
        }
        if (!player.eligible()) {
            return rejected(player.rejectionReason());
        }

        String skillPermission = skillPermission(skill);
        if (!skillPermission.isBlank() && !player.hasPermission().test(skillPermission)) {
            return rejected("Missing skill permission " + skillPermission);
        }
        String sourcePermission = source.metadata().getOrDefault("permission", "").trim();
        if (!sourcePermission.isBlank() && !player.hasPermission().test(sourcePermission)) {
            return rejected("Missing XP source permission " + sourcePermission);
        }

        Map<String, String> context = player.enrich().apply(
                request.context(), request.skillId());
        return progression.award(new XpAwardRequest(
                request.playerId(),
                request.skillId(),
                request.sourceId(),
                request.rawXp(),
                context));
    }

    private static String skillPermission(SkillDefinition skill) {
        String explicit = skill.metadata().getOrDefault("permission", "").trim();
        if (!explicit.isBlank()) {
            return explicit;
        }
        if (skill.id().namespace().equals("fabricmmo")) {
            return "mcmmo.skills." + skill.id().path();
        }
        return "";
    }

    private static XpAwardResult rejected(String detail) {
        return new XpAwardResult(XpAwardResult.Status.REJECTED, 0, 0, 0, detail);
    }

    @FunctionalInterface
    interface PlayerResolver {
        Optional<PlayerContext> resolve(UUID playerId);
    }

    record PlayerContext(
            boolean eligible,
            String rejectionReason,
            Predicate<String> hasPermission,
            java.util.function.BiFunction<Map<String, String>, NamespacedId, Map<String, String>> enrich) {
        PlayerContext {
            Objects.requireNonNull(rejectionReason, "rejectionReason");
            Objects.requireNonNull(hasPermission, "hasPermission");
            Objects.requireNonNull(enrich, "enrich");
        }

        static PlayerContext rejected(String reason) {
            return new PlayerContext(
                    false,
                    reason,
                    ignored -> false,
                    (base, ignored) -> Map.copyOf(base));
        }

        static PlayerContext eligible(
                Predicate<String> hasPermission,
                java.util.function.BiFunction<Map<String, String>, NamespacedId, Map<String, String>> enrich) {
            return new PlayerContext(true, "", hasPermission, enrich);
        }
    }
}
