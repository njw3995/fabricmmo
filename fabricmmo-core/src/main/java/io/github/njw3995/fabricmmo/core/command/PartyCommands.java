package io.github.njw3995.fabricmmo.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.njw3995.fabricmmo.core.chat.ChatChannel;
import io.github.njw3995.fabricmmo.core.chat.SharedChatHandler;
import io.github.njw3995.fabricmmo.core.party.ItemShareCategory;
import io.github.njw3995.fabricmmo.core.party.PartyFeature;
import io.github.njw3995.fabricmmo.core.party.PartyService;
import io.github.njw3995.fabricmmo.core.party.ShareMode;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.teleport.PartyTeleportService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class PartyCommands {
    private PartyCommands() { }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandPermissionService permissions) {
        var party = dispatcher.register(CommandManager.literal("party")
                .requires(source -> playerSource(source)
                        && permissions.hasPermission(source, "mcmmo.commands.party", true))
                .executes(context -> info(context.getSource()))
                .then(CommandManager.literal("help")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.help", true))
                        .executes(context -> help(context.getSource())))
                .then(CommandManager.literal("create")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.create", true))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> create(context.getSource(),
                                        StringArgumentType.getString(context, "name"), Optional.empty()))
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(context -> create(context.getSource(),
                                                StringArgumentType.getString(context, "name"),
                                                Optional.of(StringArgumentType.getString(
                                                        context, "password")))))))
                .then(CommandManager.literal("join")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.join", true))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        systems().parties().parties().stream()
                                                .map(state -> state.name()).toList(), builder))
                                .executes(context -> join(context.getSource(),
                                        StringArgumentType.getString(context, "name"), Optional.empty()))
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(context -> join(context.getSource(),
                                                StringArgumentType.getString(context, "name"),
                                                Optional.of(StringArgumentType.getString(
                                                        context, "password")))))))
                .then(CommandManager.literal("invite")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.invite", true))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(SharedCommandUtil::suggestPlayers)
                                .executes(context -> inviteMember(context.getSource(),
                                        StringArgumentType.getString(context, "player")))))
                .then(CommandManager.literal("accept")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.accept", true))
                        .executes(context -> result(context.getSource(),
                                systems().parties().acceptMemberInvite(
                                        player(context.getSource()).getUuid()),
                                "Party invitation accepted.")))
                .then(CommandManager.literal("quit")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.quit", true))
                        .executes(context -> result(context.getSource(),
                                systems().parties().leave(player(context.getSource()).getUuid()),
                                "You left the party.")))
                .then(CommandManager.literal("disband")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.disband", true))
                        .executes(context -> result(context.getSource(),
                                systems().parties().disband(player(context.getSource()).getUuid()),
                                "Party disbanded.")))
                .then(CommandManager.literal("info")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.info", true))
                        .executes(context -> info(context.getSource())))
                .then(CommandManager.literal("kick")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.kick", true))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(SharedCommandUtil::suggestPlayers)
                                .executes(context -> withTarget(context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        (actor, target) -> systems().parties().kick(actor, target),
                                        "Player removed from the party."))))
                .then(CommandManager.literal("owner")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.owner", true))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(SharedCommandUtil::suggestPlayers)
                                .executes(context -> withTarget(context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        (actor, target) -> systems().parties().changeOwner(actor, target),
                                        "Party ownership transferred."))))
                .then(CommandManager.literal("rename")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.rename", true))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> result(context.getSource(),
                                        systems().parties().rename(
                                                player(context.getSource()).getUuid(),
                                                StringArgumentType.getString(context, "name")),
                                        "Party renamed."))))
                .then(CommandManager.literal("password")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.password", true))
                        .then(CommandManager.literal("clear")
                                .executes(context -> result(context.getSource(),
                                        systems().parties().setPassword(
                                                player(context.getSource()).getUuid(), Optional.empty()),
                                        "Party password cleared.")))
                        .then(CommandManager.argument("password", StringArgumentType.word())
                                .executes(context -> result(context.getSource(),
                                        systems().parties().setPassword(
                                                player(context.getSource()).getUuid(),
                                                Optional.of(StringArgumentType.getString(
                                                        context, "password"))),
                                        "Party password updated."))))
                .then(CommandManager.literal("lock")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.lock", true))
                        .executes(context -> result(context.getSource(),
                                systems().parties().setLocked(
                                        player(context.getSource()).getUuid(), true),
                                "Party locked.")))
                .then(CommandManager.literal("unlock")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.unlock", true))
                        .executes(context -> result(context.getSource(),
                                systems().parties().setLocked(
                                        player(context.getSource()).getUuid(), false),
                                "Party unlocked.")))
                .then(CommandManager.literal("xpshare")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.xpshare", true))
                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        java.util.List.of("none", "equal"), builder))
                                .executes(context -> xpShare(context.getSource(),
                                        StringArgumentType.getString(context, "mode")))))
                .then(CommandManager.literal("itemshare")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.itemshare", true))
                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                        java.util.List.of("none", "equal", "random"), builder))
                                .executes(context -> itemShare(context.getSource(),
                                        StringArgumentType.getString(context, "mode"))))
                        .then(CommandManager.literal("category")
                                .then(CommandManager.argument("category", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                java.util.Arrays.stream(ItemShareCategory.values())
                                                        .map(value -> value.name().toLowerCase(
                                                                java.util.Locale.ROOT)).toList(),
                                                builder))
                                        .then(CommandManager.argument("enabled", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        java.util.List.of("true", "false"), builder))
                                                .executes(context -> itemCategory(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context, "category"),
                                                        StringArgumentType.getString(
                                                                context, "enabled")))))))
                .then(CommandManager.literal("alliance")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.alliance", true))
                        .then(CommandManager.literal("invite")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .suggests(SharedCommandUtil::suggestPlayers)
                                        .executes(context -> allianceInvite(context.getSource(),
                                                StringArgumentType.getString(context, "player")))))
                        .then(CommandManager.literal("accept")
                                .executes(context -> result(context.getSource(),
                                        systems().parties().acceptAlliance(
                                                player(context.getSource()).getUuid()),
                                        "Party alliance formed.")))
                        .then(CommandManager.literal("disband")
                                .executes(context -> result(context.getSource(),
                                        systems().parties().disbandAlliance(
                                                player(context.getSource()).getUuid()),
                                        "Party alliance disbanded."))))
                .then(CommandManager.literal("chat")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.chat", true))
                        .executes(context -> toggleChat(context.getSource(), ChatChannel.PARTY)))
                .then(CommandManager.literal("teleport")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.party.teleport", true))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(SharedCommandUtil::suggestPlayers)
                                .executes(context -> requestTeleport(context.getSource(),
                                        StringArgumentType.getString(context, "player"))))));

        var partyChat = dispatcher.register(CommandManager.literal("partychat")
                .requires(source -> permissions.hasPermission(
                        source, "mcmmo.chat.partychat", true))
                .executes(context -> toggleChat(context.getSource(), ChatChannel.PARTY))
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(context -> directPartyMessage(
                                context.getSource(),
                                StringArgumentType.getString(context, "message"),
                                permissions))));
        dispatcher.register(CommandManager.literal("pc").redirect(partyChat));
        dispatcher.register(CommandManager.literal("p").redirect(partyChat));
        dispatcher.register(CommandManager.literal("pchat").redirect(partyChat));

        var adminChat = dispatcher.register(CommandManager.literal("adminchat")
                .requires(source -> permissions.hasPermission(
                        source, "mcmmo.chat.adminchat", 2))
                .executes(context -> toggleChat(context.getSource(), ChatChannel.ADMIN))
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(context -> directAdminMessage(
                                context.getSource(),
                                StringArgumentType.getString(context, "message"),
                                permissions))));
        dispatcher.register(CommandManager.literal("ac").redirect(adminChat));
        dispatcher.register(CommandManager.literal("a").redirect(adminChat));
        dispatcher.register(CommandManager.literal("achat").redirect(adminChat));

        dispatcher.register(CommandManager.literal("mcchatspy")
                .requires(source -> permissions.hasPermission(
                        source, "mcmmo.commands.mcchatspy", 2)
                        || permissions.hasPermission(
                                source, "mcmmo.commands.mcchatspy.others", 2))
                .executes(context -> spySelf(context.getSource(), permissions))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.mcchatspy.others", 2))
                        .suggests(SharedCommandUtil::suggestPlayers)
                        .executes(context -> spyOther(
                                context.getSource(),
                                StringArgumentType.getString(context, "player")))));

        dispatcher.register(CommandManager.literal("ptp")
                .requires(source -> playerSource(source)
                        && permissions.hasPermission(source, "mcmmo.commands.ptp", true))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.ptp.send", true))
                        .suggests(SharedCommandUtil::suggestPlayers)
                        .executes(context -> requestTeleport(context.getSource(),
                                StringArgumentType.getString(context, "player"))))
                .then(CommandManager.literal("toggle")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.ptp.toggle", true))
                        .executes(context -> toggleTeleport(context.getSource())))
                .then(CommandManager.literal("accept")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.ptp.accept", true))
                        .executes(context -> acceptTeleport(context.getSource(), Optional.empty()))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(SharedCommandUtil::suggestPlayers)
                                .executes(context -> acceptTeleport(context.getSource(),
                                        Optional.of(StringArgumentType.getString(
                                                context, "player"))))))
                .then(CommandManager.literal("acceptany")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.ptp.acceptall", true))
                        .executes(context -> toggleAcceptAny(context.getSource())))
                .then(CommandManager.literal("acceptall")
                        .requires(source -> permissions.hasPermission(
                                source, "mcmmo.commands.ptp.acceptall", true))
                        .executes(context -> toggleAcceptAny(context.getSource()))));
    }

    private static int help(ServerCommandSource source) {
        source.sendMessage(Text.literal("/party create|join|invite|accept|quit|info").formatted(Formatting.GOLD));
        source.sendMessage(Text.literal("/party xpshare <none|equal>"));
        source.sendMessage(Text.literal("/party itemshare <none|equal|random>"));
        source.sendMessage(Text.literal("/party alliance <invite|accept|disband>"));
        source.sendMessage(Text.literal("/ptp <player> | /ptp accept [player]"));
        return 1;
    }

    private static io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems.State systems() {
        return SharedCommandUtil.systems();
    }
    private static boolean playerSource(ServerCommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity;
    }
    private static ServerPlayerEntity player(ServerCommandSource source) {
        try { return source.getPlayerOrThrow(); }
        catch (Exception exception) { throw new IllegalStateException("Player-only command", exception); }
    }
    private static int result(ServerCommandSource source, PartyService.Result result,
            String successMessage) {
        return result.success() ? SharedCommandUtil.success(source, successMessage)
                : SharedCommandUtil.error(source, result.detail());
    }
    private static int create(ServerCommandSource source, String name, Optional<String> password) {
        return result(source, systems().parties().create(player(source).getUuid(), name, password),
                "Party created.");
    }
    private static int join(ServerCommandSource source, String name, Optional<String> password) {
        return result(source, systems().parties().join(player(source).getUuid(), name, password),
                "You joined " + name + '.');
    }
    private static int inviteMember(ServerCommandSource source, String name) {
        var id = SharedCommandUtil.playerId(source, name);
        if (id.isEmpty()) return SharedCommandUtil.error(source, "Player not found: " + name);
        PartyService.Result result = systems().parties().inviteMember(
                player(source).getUuid(), id.orElseThrow());
        if (result.success()) {
            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(id.orElseThrow());
            if (target != null) target.sendMessage(Text.literal(
                    player(source).getGameProfile().getName()
                            + " invited you to party " + result.party().orElseThrow().name()
                            + ". Use /party accept."));
        }
        return result(source, result, "Party invitation sent.");
    }
    private static int withTarget(ServerCommandSource source, String name,
            java.util.function.BiFunction<UUID, UUID, PartyService.Result> operation,
            String successMessage) {
        var id = SharedCommandUtil.playerId(source, name);
        if (id.isEmpty()) return SharedCommandUtil.error(source, "Player not found: " + name);
        return result(source, operation.apply(player(source).getUuid(), id.orElseThrow()), successMessage);
    }
    private static int info(ServerCommandSource source) {
        var state = systems().parties().partyOf(player(source).getUuid()).orElse(null);
        if (state == null) return SharedCommandUtil.error(source, "You are not in a party.");
        source.sendMessage(Text.literal("---- Party: " + state.name() + " ----").formatted(Formatting.GOLD));
        source.sendMessage(Text.literal("Owner: " + SharedCommandUtil.playerName(state.owner())));
        source.sendMessage(Text.literal("Level: " + state.level() + "  XP: "
                + visible(state.xp()) + "/" + systems().parties().xpToNextLevel(
                        state, onlineMembers(state.members()))));
        source.sendMessage(Text.literal("Members: " + state.members().stream()
                .map(SharedCommandUtil::playerName).sorted().toList()));
        source.sendMessage(Text.literal("Locked: " + state.locked() + "  XP Share: "
                + state.xpShare() + "  Item Share: " + state.itemShare()));
        source.sendMessage(Text.literal("Item categories: " + state.itemShareCategories()));
        state.alliance().ifPresent(ally -> source.sendMessage(Text.literal("Alliance: " + ally)));
        for (PartyFeature feature : PartyFeature.values()) {
            boolean unlocked = systems().parties().featureUnlocked(state, feature);
            source.sendMessage(Text.literal(feature + ": " + (unlocked ? "UNLOCKED"
                    : "level " + systems().parties().settings().unlockLevel(feature)))
                    .formatted(unlocked ? Formatting.GREEN : Formatting.GRAY));
        }
        return 1;
    }
    private static int xpShare(ServerCommandSource source, String mode) {
        try { ShareMode parsed = ShareMode.parse(mode); return result(source,
                systems().parties().setXpShare(player(source).getUuid(), parsed),
                "Party XP sharing set to " + parsed + '.'); }
        catch (IllegalArgumentException exception) { return SharedCommandUtil.error(source, exception.getMessage()); }
    }
    private static int itemShare(ServerCommandSource source, String mode) {
        try { ShareMode parsed = ShareMode.parse(mode); return result(source,
                systems().parties().setItemShare(player(source).getUuid(), parsed),
                "Party item sharing set to " + parsed + '.'); }
        catch (IllegalArgumentException exception) { return SharedCommandUtil.error(source, exception.getMessage()); }
    }
    private static int itemCategory(ServerCommandSource source, String category, String enabled) {
        try { ItemShareCategory parsed = ItemShareCategory.parse(category);
            boolean value = SharedCommandUtil.parseBoolean(enabled);
            return result(source, systems().parties().setItemShareCategory(
                    player(source).getUuid(), parsed, value),
                    parsed + " item sharing " + (value ? "enabled" : "disabled") + '.'); }
        catch (IllegalArgumentException exception) { return SharedCommandUtil.error(source, exception.getMessage()); }
    }
    private static int allianceInvite(ServerCommandSource source, String targetName) {
        var id = SharedCommandUtil.playerId(source, targetName);
        if (id.isEmpty()) return SharedCommandUtil.error(source, "Player not found: " + targetName);
        var targetParty = systems().parties().partyOf(id.orElseThrow()).orElse(null);
        if (targetParty == null) return SharedCommandUtil.error(source, "That player is not in a party.");
        if (!targetParty.owner().equals(id.orElseThrow())) return SharedCommandUtil.error(
                source, "The target player is not their party owner.");
        PartyService.Result result = systems().parties().inviteAlliance(
                player(source).getUuid(), targetParty.name());
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(id.orElseThrow());
        if (result.success() && target != null) target.sendMessage(Text.literal(
                "Party " + systems().parties().partyOf(player(source).getUuid()).orElseThrow().name()
                        + " requested an alliance. Use /party alliance accept."));
        return result(source, result, "Alliance invitation sent.");
    }
    private static int toggleChat(ServerCommandSource source, ChatChannel channel) {
        if (!playerSource(source)) {
            return SharedCommandUtil.error(source,
                    "Console cannot switch chat channels; provide a message instead.");
        }
        ServerPlayerEntity player = player(source);
        if (channel == ChatChannel.PARTY) {
            var party = systems().parties().partyOf(player.getUuid()).orElse(null);
            if (party == null) return SharedCommandUtil.error(source, "You are not in a party.");
            if (!systems().parties().featureUnlocked(party, PartyFeature.CHAT)) return SharedCommandUtil.error(
                    source, "Party chat unlocks at party level "
                            + systems().parties().settings().unlockLevel(PartyFeature.CHAT) + '.');
            if (!systems().chatSettings().partyEnabled()) return SharedCommandUtil.error(
                    source, "Party chat is disabled by the server.");
        } else if (!systems().chatSettings().adminEnabled()) {
            return SharedCommandUtil.error(source, "Admin chat is disabled by the server.");
        }
        ChatChannel active = systems().chats().toggle(player.getUuid(), channel);
        String key = active == ChatChannel.NONE ? "Chat.Channel.Off" : "Chat.Channel.On";
        return SharedCommandUtil.success(source, LegacyText.strip(systems().locale().text(
                key, active == ChatChannel.NONE ? ""
                        : active.name().toLowerCase(java.util.Locale.ROOT))));
    }
    private static int directPartyMessage(
            ServerCommandSource source,
            String message,
            CommandPermissionService permissions) {
        if (message.isBlank()) return SharedCommandUtil.error(source, "Message cannot be blank.");
        if (playerSource(source)) {
            ServerPlayerEntity sender = player(source);
            var party = systems().parties().partyOf(sender.getUuid()).orElse(null);
            if (party == null) return SharedCommandUtil.error(source, "You are not in a party.");
            if (!systems().parties().featureUnlocked(party, PartyFeature.CHAT)) {
                return SharedCommandUtil.error(source, "Party chat unlocks at party level "
                        + systems().parties().settings().unlockLevel(PartyFeature.CHAT) + '.');
            }
            return SharedChatHandler.sendPlayerMessage(
                    sender, ChatChannel.PARTY, message, permissions) ? 1 : 0;
        }
        int separator = message.indexOf(' ');
        if (separator <= 0 || separator == message.length() - 1) {
            return SharedCommandUtil.error(source,
                    "Console usage: /partychat <party> <message>");
        }
        String partyName = message.substring(0, separator);
        String content = message.substring(separator + 1);
        return SharedChatHandler.sendConsoleParty(source.getServer(), partyName, content)
                ? 1 : SharedCommandUtil.error(source, "Party chat could not be delivered.");
    }

    private static int directAdminMessage(
            ServerCommandSource source,
            String message,
            CommandPermissionService permissions) {
        if (message.isBlank()) return SharedCommandUtil.error(source, "Message cannot be blank.");
        if (playerSource(source)) {
            return SharedChatHandler.sendPlayerMessage(
                    player(source), ChatChannel.ADMIN, message, permissions) ? 1 : 0;
        }
        return SharedChatHandler.sendConsoleAdmin(source.getServer(), message, permissions)
                ? 1 : SharedCommandUtil.error(source, "Admin chat could not be delivered.");
    }

    private static int spySelf(
            ServerCommandSource source,
            CommandPermissionService permissions) {
        if (!playerSource(source)) {
            return SharedCommandUtil.error(source, "Console usage: /mcchatspy <player>");
        }
        if (!permissions.hasPermission(source, "mcmmo.commands.mcchatspy", 2)) {
            return SharedCommandUtil.error(source, "You do not have permission to use this command.");
        }
        boolean enabled = systems().chats().toggleSpy(player(source).getUuid());
        return SharedCommandUtil.success(source, LegacyText.strip(systems().locale().text(
                enabled ? "Commands.AdminChatSpy.Enabled" : "Commands.AdminChatSpy.Disabled")));
    }

    private static int spyOther(ServerCommandSource source, String targetName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
        if (target == null) {
            return SharedCommandUtil.error(source, "Player must be online: " + targetName);
        }
        boolean enabled = systems().chats().toggleSpy(target.getUuid());
        target.sendMessage(LegacyText.parse(systems().locale().text(
                enabled ? "Commands.AdminChatSpy.Enabled" : "Commands.AdminChatSpy.Disabled")));
        return SharedCommandUtil.success(source, LegacyText.strip(systems().locale().text(
                "Commands.AdminChatSpy.Toggle", target.getGameProfile().getName())));
    }
    private static int requestTeleport(ServerCommandSource source, String name) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(name);
        if (target == null) return SharedCommandUtil.error(source, "Player must be online: " + name);
        PartyTeleportService.Result result = systems().teleports().request(player(source), target);
        if (result.success() && result.detail().equals("Teleport request sent.")) {
            target.sendMessage(Text.literal(player(source).getGameProfile().getName()
                    + " requested a party teleport. Use /ptp accept."));
        }
        return result.success() ? SharedCommandUtil.success(source, result.detail())
                : SharedCommandUtil.error(source, result.detail());
    }
    private static int acceptTeleport(ServerCommandSource source, Optional<String> name) {
        Optional<UUID> requester = name.flatMap(value -> SharedCommandUtil.playerId(source, value));
        PartyTeleportService.Result result = systems().teleports().accept(player(source), requester);
        return result.success() ? SharedCommandUtil.success(source, result.detail())
                : SharedCommandUtil.error(source, result.detail());
    }
    private static int toggleTeleport(ServerCommandSource source) {
        var preference = systems().teleports().toggleEnabled(player(source).getUuid());
        return SharedCommandUtil.success(source,
                "Party teleport " + (preference.enabled() ? "enabled" : "disabled") + '.');
    }
    private static int toggleAcceptAny(ServerCommandSource source) {
        var preference = systems().teleports().toggleAcceptAny(player(source).getUuid());
        return SharedCommandUtil.success(source, preference.confirmRequired()
                ? "Party teleports now require individual acceptance."
                : "Party teleports will now be accepted automatically.");
    }
    private static int onlineMembers(java.util.Set<UUID> members) {
        int count = 0; for (UUID member : members) if (
                systems().server().getPlayerManager().getPlayer(member) != null) count++; return count;
    }
    private static int visible(double value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.floor(value);
    }
}
