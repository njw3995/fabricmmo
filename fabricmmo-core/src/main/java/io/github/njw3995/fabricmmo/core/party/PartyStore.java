package io.github.njw3995.fabricmmo.core.party;

import java.io.IOException;
import java.util.Map;

public interface PartyStore {
    Map<String, PartyState> load() throws IOException;
    void save(Map<String, PartyState> parties) throws IOException;
}
