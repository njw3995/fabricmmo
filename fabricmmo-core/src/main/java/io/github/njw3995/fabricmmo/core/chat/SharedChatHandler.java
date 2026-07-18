package io.github.njw3995.fabricmmo.core.chat;

import io.github.njw3995.fabricmmo.core.command.LegacyText;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
import io.github.njw3995.fabricmmo.core.party.PartyFeature;
import io.github.njw3995.fabricmmo.core.party.PartyState;
import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Upstream-configured party/admin chat interception and direct-command delivery. */
public final class SharedChatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO-Chat");

    private SharedChatHandler() { }

    public static void register(CommandPermissionService permissions) {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!SharedServerSystems.running()) return true;
            ChatChannel channel = SharedServerSystems.require().chats().channel(sender.getUuid());
            if (channel == ChatChannel.NONE) return true;
            sendPlayerMessage(sender, channel, message.getContent().getString(), permissions);
            return false;
        });
    }

    public static boolean sendPlayerMessage(
            ServerPlayerEntity sender,
            ChatChannel channel,
            String rawContent,
            CommandPermissionService permissions) {
        if (!SharedServerSystems.running()) return false;
        var systems = SharedServerSystems.require();
        ChatSettings settings = systems.chatSettings();
        if (!settings.enabled()) {
            sender.sendMessage(Text.literal("mcMMO chat is disabled by the server.")
                    .formatted(Formatting.RED));
            return false;
        }
        boolean colors = permissions.hasPermission(
                sender.getCommandSource(), "mcmmo.chat.colors", false);
        Text content = colors
                ? LegacyText.parse(rawContent)
                : Text.literal(literalize(rawContent));
        return switch (channel) {
            case PARTY -> handleParty(sender, content, rawContent, settings);
            case ADMIN -> handleAdmin(sender, content, rawContent, settings, permissions);
            case NONE -> false;
        };
    }

    public static boolean sendConsoleAdmin(
            MinecraftServer server,
            String rawContent,
            CommandPermissionService permissions) {
        if (!SharedServerSystems.running()) return false;
        var systems = SharedServerSystems.require();
        ChatSettings settings = systems.chatSettings();
        if (!settings.enabled() || !settings.adminEnabled()) return false;
        String author = consoleName();
        Text formatted = formatMessage(
                systems.locale().text("Chat.Style.Admin", author, "{MESSAGE}"),
                LegacyText.parse(rawContent));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (permissions.hasPermission(
                    player.getCommandSource(), "mcmmo.chat.adminchat", false)) {
                player.sendMessage(formatted);
            }
        }
        if (settings.adminSendToConsole()) {
            LOGGER.info("{}", LegacyText.strip(
                    systems.locale().text("Chat.Style.Admin", author, rawContent)));
        }
        return true;
    }

    public static boolean sendConsoleParty(
            MinecraftServer server,
            String partyName,
            String rawContent) {
        if (!SharedServerSystems.running()) return false;
        var systems = SharedServerSystems.require();
        ChatSettings settings = systems.chatSettings();
        PartyState party = systems.parties().party(partyName).orElse(null);
        if (!settings.enabled() || !settings.partyEnabled() || party == null) return false;
        String author = consoleName();
        Text content = LegacyText.parse(rawContent);
        Text formatted = formatMessage(
                systems.locale().text("Chat.Style.Party", author, "{MESSAGE}"), content);
        for (UUID memberId : party.members()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
            if (member != null) member.sendMessage(formatted);
        }
        sendPartySpyMessages(party, author, content);
        if (settings.partySendToConsole()) {
            LOGGER.info("{}", LegacyText.strip(systems.locale().text(
                    "Chat.Spy.Party", author, rawContent, party.name())));
        }
        return true;
    }

    private static boolean handleParty(
            ServerPlayerEntity sender,
            Text content,
            String rawContent,
            ChatSettings settings) {
        var systems = SharedServerSystems.require();
        if (!settings.partyEnabled()) {
            sender.sendMessage(Text.literal("Party chat is disabled by the server.")
                    .formatted(Formatting.RED));
            systems.chats().set(sender.getUuid(), ChatChannel.NONE);
            return false;
        }
        PartyState party = systems.parties().partyOf(sender.getUuid()).orElse(null);
        if (party == null) {
            sender.sendMessage(Text.literal("You are not in a party.").formatted(Formatting.RED));
            systems.chats().set(sender.getUuid(), ChatChannel.NONE);
            return false;
        }
        if (!systems.parties().featureUnlocked(party, PartyFeature.CHAT)) {
            sender.sendMessage(Text.literal("Party chat unlocks at party level "
                    + systems.parties().settings().unlockLevel(PartyFeature.CHAT) + ".")
                    .formatted(Formatting.RED));
            systems.chats().set(sender.getUuid(), ChatChannel.NONE);
            return false;
        }
        String author = authorName(sender, settings.partyUseDisplayNames());
        String key = party.owner().equals(sender.getUuid())
                ? "Chat.Style.Party.Leader" : "Chat.Style.Party";
        Text formatted = formatMessage(systems.locale().text(key, author, "{MESSAGE}"), content);
        for (UUID recipientId : party.members()) {
            ServerPlayerEntity recipient = systems.server().getPlayerManager().getPlayer(recipientId);
            if (recipient != null) recipient.sendMessage(formatted);
        }
        sendPartySpyMessages(party, author, content);
        if (settings.partySendToConsole()) {
            LOGGER.info("{}", LegacyText.strip(systems.locale().text(
                    "Chat.Spy.Party", author, literalize(rawContent), party.name())));
        }
        return true;
    }

    private static void sendPartySpyMessages(PartyState party, String author, Text content) {
        var systems = SharedServerSystems.require();
        Text spyMessage = formatMessage(
                systems.locale().text("Chat.Spy.Party", author, "{MESSAGE}", party.name()),
                content);
        Set<UUID> recipients = new HashSet<>(systems.chats().spies());
        for (UUID spyId : recipients) {
            if (party.members().contains(spyId)) continue;
            ServerPlayerEntity spy = systems.server().getPlayerManager().getPlayer(spyId);
            if (spy != null) spy.sendMessage(spyMessage);
        }
    }

    private static boolean handleAdmin(
            ServerPlayerEntity sender,
            Text content,
            String rawContent,
            ChatSettings settings,
            CommandPermissionService permissions) {
        var systems = SharedServerSystems.require();
        if (!settings.adminEnabled()) {
            sender.sendMessage(Text.literal("Admin chat is disabled by the server.")
                    .formatted(Formatting.RED));
            systems.chats().set(sender.getUuid(), ChatChannel.NONE);
            return false;
        }
        String author = authorName(sender, settings.adminUseDisplayNames());
        Text formatted = formatMessage(
                systems.locale().text("Chat.Style.Admin", author, "{MESSAGE}"), content);
        for (ServerPlayerEntity player : systems.server().getPlayerManager().getPlayerList()) {
            if (permissions.hasPermission(
                    player.getCommandSource(), "mcmmo.chat.adminchat", false)) {
                player.sendMessage(formatted);
            }
        }
        if (settings.adminSendToConsole()) {
            LOGGER.info("{}", LegacyText.strip(systems.locale().text(
                    "Chat.Style.Admin", author, literalize(rawContent))));
        }
        return true;
    }

    private static Text formatMessage(String template, Text message) {
        int marker = template.indexOf("{MESSAGE}");
        if (marker < 0) return LegacyText.parse(template).copy().append(message);
        return LegacyText.parse(template.substring(0, marker)).copy()
                .append(message)
                .append(LegacyText.parse(
                        template.substring(marker + "{MESSAGE}".length())));
    }

    private static String authorName(ServerPlayerEntity player, boolean displayName) {
        return displayName
                ? player.getDisplayName().getString()
                : player.getGameProfile().getName();
    }

    private static String consoleName() {
        var systems = SharedServerSystems.require();
        return LegacyText.strip(systems.locale().text("Chat.Identity.Console"));
    }

    private static String literalize(String input) {
        return input == null ? "" : input.replace('\u00A7', '&');
    }
}
