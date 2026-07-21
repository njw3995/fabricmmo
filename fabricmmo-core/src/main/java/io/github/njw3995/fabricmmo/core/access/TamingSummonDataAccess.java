package io.github.njw3995.fabricmmo.core.access;

import java.util.UUID;

public interface TamingSummonDataAccess {
    UUID fabricmmo$summonOwner();
    String fabricmmo$summonType();
    long fabricmmo$summonExpiresAt();
    void fabricmmo$setSummonData(UUID owner, String type, long expiresAt);
    void fabricmmo$clearSummonData();
}
