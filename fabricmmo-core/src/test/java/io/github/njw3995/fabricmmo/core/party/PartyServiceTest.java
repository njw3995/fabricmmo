package io.github.njw3995.fabricmmo.core.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.progression.XpCurve;
import io.github.njw3995.fabricmmo.core.progression.ProgressionFormula;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PartyServiceTest {
    @TempDir
    Path directory;

    @Test
    void persistsMembershipProgressSharingAndAlliances() throws Exception {
        UUID alphaOwner = UUID.randomUUID();
        UUID alphaMember = UUID.randomUUID();
        UUID betaOwner = UUID.randomUUID();
        PropertiesPartyStore store = new PropertiesPartyStore(directory.resolve("parties.properties"));
        PartyService service = service(store);

        assertTrue(service.create(alphaOwner, "Alpha", Optional.empty()).success());
        assertTrue(service.inviteMember(alphaOwner, alphaMember).success());
        assertTrue(service.acceptMemberInvite(alphaMember).success());
        assertTrue(service.create(betaOwner, "Beta", Optional.empty()).success());

        levelToCap(service, alphaOwner);
        levelToCap(service, betaOwner);
        assertTrue(service.setXpShare(alphaOwner, ShareMode.EQUAL).success());
        assertTrue(service.setItemShare(alphaOwner, ShareMode.RANDOM).success());
        assertTrue(service.setItemShareCategory(
                alphaOwner, ItemShareCategory.MISC, false).success());
        assertTrue(service.inviteAlliance(alphaOwner, "Beta").success());
        assertTrue(service.acceptAlliance(betaOwner).success());

        PartyService reloaded = service(store);
        PartyState alpha = reloaded.party("Alpha").orElseThrow();
        PartyState beta = reloaded.party("Beta").orElseThrow();
        assertEquals(2, alpha.members().size());
        assertEquals(ShareMode.EQUAL, alpha.xpShare());
        assertEquals(ShareMode.RANDOM, alpha.itemShare());
        assertFalse(alpha.itemShareCategories().contains(ItemShareCategory.MISC));
        assertEquals(Optional.of("Beta"), alpha.alliance());
        assertEquals(Optional.of("Alpha"), beta.alliance());
    }

    @Test
    void featureUnlocksRejectEarlyUse() throws Exception {
        UUID owner = UUID.randomUUID();
        PartyService service = service(new PropertiesPartyStore(
                directory.resolve("locked.properties")));
        service.create(owner, "NewParty", Optional.empty());

        assertFalse(service.setXpShare(owner, ShareMode.EQUAL).success());
        assertFalse(service.setItemShare(owner, ShareMode.EQUAL).success());
        assertFalse(service.inviteAlliance(owner, "missing").success());
    }

    private static PartyService service(PartyStore store) throws Exception {
        return new PartyService(
                store,
                PartySettings.upstreamDefaults(),
                new ProgressionFormula(XpCurve.upstreamDefaults()),
                ProgressionMode.RETRO,
                FormulaType.LINEAR);
    }

    private static void levelToCap(PartyService service, UUID owner) {
        for (int attempt = 0; attempt < 20
                && service.partyOf(owner).orElseThrow().level() < 10; attempt++) {
            service.applyXpGain(owner, 1_000_000.0D, 1, 0);
        }
        assertEquals(10, service.partyOf(owner).orElseThrow().level());
    }
}
