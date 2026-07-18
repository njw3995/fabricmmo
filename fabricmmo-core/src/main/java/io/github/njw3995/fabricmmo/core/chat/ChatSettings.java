package io.github.njw3995.fabricmmo.core.chat;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;

/** Exact chat.yml channel enablement, identity, console, and automatic-spy settings. */
public record ChatSettings(
        boolean enabled,
        boolean partyEnabled,
        boolean partyUseDisplayNames,
        boolean partySendToConsole,
        boolean automaticPartySpy,
        boolean adminEnabled,
        boolean adminUseDisplayNames,
        boolean adminSendToConsole) {

    public static ChatSettings load(Path file) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(file);
        return new ChatSettings(
                config.bool("Chat.Enable", true),
                config.bool("Chat.Channels.Party.Enable", true),
                config.bool("Chat.Channels.Party.Use_Display_Names", true),
                config.bool("Chat.Channels.Party.Send_To_Console", true),
                config.bool("Chat.Channels.Party.Spies.Automatically_Enable_Spying", false),
                config.bool("Chat.Channels.Admin.Enable", true),
                config.bool("Chat.Channels.Admin.Use_Display_Names", true),
                config.bool("Chat.Channels.Admin.Send_To_Console", true));
    }

    public static ChatSettings upstreamDefaults() {
        return new ChatSettings(true, true, true, true, false, true, true, true);
    }
}
