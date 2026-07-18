package io.github.njw3995.fabricmmo.core.diagnostic;

import io.github.njw3995.fabricmmo.api.event.AbilityStateEvent;
import io.github.njw3995.fabricmmo.api.event.LevelChangedEvent;
import io.github.njw3995.fabricmmo.api.event.XpAwardedEvent;
import io.github.njw3995.fabricmmo.api.event.XpPreAwardEvent;
import io.github.njw3995.fabricmmo.core.session.PlayerSessionStateService;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Runtime diagnostics emitted only to players who enabled /mmodebug. */
public final class DebugDiagnosticsService {
    private final MinecraftServer server;
    private final PlayerSessionStateService sessions;

    public DebugDiagnosticsService(MinecraftServer server, PlayerSessionStateService sessions) {
        this.server = Objects.requireNonNull(server, "server");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    public void preAward(XpPreAwardEvent event) {
        send(event.request().playerId(), "XP PRE skill=" + event.request().skillId()
                + " source=" + event.request().sourceId()
                + " raw=" + event.request().rawXp()
                + " context=" + event.request().context());
    }

    public void awarded(XpAwardedEvent event) {
        send(event.request().playerId(), "XP RESULT status=" + event.result().status()
                + " applied=" + event.result().appliedXp()
                + " level=" + event.result().oldLevel() + "->" + event.result().newLevel()
                + (event.result().detail().isBlank() ? "" : " detail=" + event.result().detail()));
    }

    public void levelChanged(LevelChangedEvent event) {
        send(event.playerId(), "LEVEL skill=" + event.skillId() + " "
                + event.oldLevel() + "->" + event.newLevel());
    }

    public void ability(AbilityStateEvent event) {
        send(event.playerId(), "ABILITY " + event.abilityId() + " " + event.state());
    }


    public void snapshot(ServerPlayerEntity player,
            io.github.njw3995.fabricmmo.api.FabricMmoApi api,
            io.github.njw3995.fabricmmo.core.ability.AbilityCooldownService cooldowns) {
        send(player.getUuid(), "PLAYER uuid=" + player.getUuid()
                + " world=" + player.getWorld().getRegistryKey().getValue()
                + " pos=" + player.getBlockPos()
                + " gameMode=" + player.interactionManager.getGameMode());
        api.skillRegistry().skills().stream().sorted(java.util.Comparator.comparing(
                skill -> skill.id().toString())).forEach(skill -> {
            var progress = api.progression().query(player.getUuid(), skill.id());
            String implementation = skill.id().path().equals("mining")
                    ? "implemented" : "placeholder-pending-skill-mechanics";
            send(player.getUuid(), "SKILL " + skill.id() + " level=" + progress.level()
                    + " xp=" + progress.xp() + '/' + progress.xpToNextLevel()
                    + " state=" + implementation);
        });
        cooldowns.remaining(player.getUuid()).forEach((id, seconds) ->
                send(player.getUuid(), "COOLDOWN " + id + " remaining=" + seconds + "s"));
    }

    public void blockDecision(java.util.UUID playerId, String blockId, int configuredXp,
            boolean creative, boolean validTool, boolean permission, boolean protection,
            boolean playerPlaced, String result) {
        send(playerId, "BLOCK block=" + blockId + " configuredXp=" + configuredXp
                + " creative=" + creative + " validTool=" + validTool
                + " permission=" + permission + " protection=" + protection
                + " playerPlaced=" + playerPlaced + " result=" + result);
    }

    public void placeholderCombat(java.util.UUID playerId, String mechanic, String detail) {
        send(playerId, "COMBAT mechanic=" + mechanic
                + " state=placeholder-pending-skill-mechanics " + detail);
    }

    public void message(java.util.UUID playerId, String detail) {
        send(playerId, detail);
    }

    private void send(java.util.UUID playerId, String detail) {
        if (!sessions.get(playerId).debug()) {
            return;
        }
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player != null) {
            player.sendMessage(Text.literal("[mcMMO Debug] ").formatted(Formatting.DARK_AQUA)
                    .append(Text.literal(detail).formatted(Formatting.GRAY)));
        }
    }
}
