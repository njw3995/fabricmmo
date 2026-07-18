package io.github.njw3995.fabricmmo.core.chat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatStateService {
    private final Map<UUID, ChatChannel> channels = new HashMap<>();
    private final Set<UUID> spies = new HashSet<>();
    public synchronized ChatChannel channel(UUID id) { return channels.getOrDefault(id, ChatChannel.NONE); }
    public synchronized void set(UUID id, ChatChannel channel) {
        if (channel == ChatChannel.NONE) channels.remove(id); else channels.put(id, channel);
    }
    public synchronized ChatChannel toggle(UUID id, ChatChannel channel) {
        ChatChannel next = channel(id) == channel ? ChatChannel.NONE : channel;
        if (next == ChatChannel.NONE) channels.remove(id); else channels.put(id, next);
        return next;
    }
    public synchronized boolean toggleSpy(UUID id) { if (!spies.add(id)) { spies.remove(id); return false; } return true; }
    public synchronized boolean spying(UUID id) { return spies.contains(id); }
    public synchronized Set<UUID> spies() { return Set.copyOf(spies); }
    public synchronized void remove(UUID id) { channels.remove(id); spies.remove(id); }
    public synchronized void clear() { channels.clear(); spies.clear(); }
}
