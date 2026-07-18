package io.github.njw3995.fabricmmo.core.command;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

final class SharedCommandUtil {
    private SharedCommandUtil() { }
    static FabricMmoApi api(){return FabricMmoFabricRuntime.requireApi();}
    static SharedServerSystems.State systems(){return SharedServerSystems.require();}
    static Optional<SkillDefinition> skill(String value, boolean children){return SkillArgumentResolver.resolve(api().skillRegistry(),value,children);}
    static Set<String> skills(boolean all){return new LinkedHashSet<>(SkillArgumentResolver.suggestions(api().skillRegistry(),all));}
    static Optional<UUID> playerId(ServerCommandSource source,String name){ServerPlayerEntity online=source.getServer().getPlayerManager().getPlayer(name);if(online!=null)return Optional.of(online.getUuid());try{return Optional.of(UUID.fromString(name));}catch(IllegalArgumentException ignored){return systems().identities().findUuid(name);}}
    static String playerName(UUID id){ServerPlayerEntity p=systems().server().getPlayerManager().getPlayer(id);return p==null?systems().identities().findName(id).orElse(id.toString()):p.getGameProfile().getName();}
    static CompletableFuture<Suggestions> suggestSkills(CommandContext<ServerCommandSource> c,SuggestionsBuilder b){return CommandSource.suggestMatching(skills(true),b);}
    static CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        ServerCommandSource source = context.getSource();
        LinkedHashSet<String> names = new LinkedHashSet<>();
        ServerPlayerEntity viewer = source.getEntity() instanceof ServerPlayerEntity player
                ? player : null;
        boolean seeHidden = systems().permissions().hasPermission(
                source, "mcmmo.commands.inspect.hidden", 2);
        for (ServerPlayerEntity candidate : source.getServer().getPlayerManager().getPlayerList()) {
            if (viewer == null || seeHidden || systems().visibility().visibleTo(candidate, viewer)) {
                names.add(candidate.getGameProfile().getName());
            }
        }
        for (var identity : systems().identities().identities().entrySet()) {
            ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(identity.getKey());
            if (online == null || viewer == null || seeHidden
                    || systems().visibility().visibleTo(online, viewer)) {
                names.add(identity.getValue());
            }
        }
        return CommandSource.suggestMatching(names, builder);
    }
    static String cap(String value){String[] w=value.replace('-','_').split("_");StringBuilder b=new StringBuilder();for(String x:w){if(x.isEmpty())continue;if(!b.isEmpty())b.append(' ');b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1).toLowerCase(Locale.ROOT));}return b.toString();}
    static int error(ServerCommandSource s,String message){s.sendError(Text.literal(message));return 0;}
    static int success(ServerCommandSource s,String message){s.sendMessage(Text.literal(message));return 1;}
    static boolean parseBoolean(String v){return v.equalsIgnoreCase("true")||v.equalsIgnoreCase("on")||v.equalsIgnoreCase("enabled");}
    static NamespacedId coreSkill(String path){return new NamespacedId("fabricmmo",path.toLowerCase(Locale.ROOT));}
}
