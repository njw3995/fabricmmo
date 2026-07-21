package io.github.njw3995.fabricmmo.core.skill.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TamingXpTableTest {
    @TempDir Path temp;

    @Test
    void loadsPinnedDefaultsAndConfiguredOverrides() throws Exception {
        Path file = temp.resolve("experience.yml");
        Files.writeString(file, """
                Experience_Values:
                  Taming:
                    Animal_Taming:
                      Wolf: 333
                      Modded_Beast: 42.5
                """);
        TamingXpTable table = TamingXpTable.load(file);
        assertEquals(333.0D, table.xp("wolf"));
        assertEquals(42.5D, table.xp("MODDED_BEAST"));
        assertEquals(1300.0D, table.xp("camel"));
        assertEquals(0.0D, table.xp("unknown"));
    }

    @Test
    void rejectsCorruptConfiguredXpInsteadOfSilentlyResettingIt() throws Exception {
        Path file = temp.resolve("experience.yml");
        Files.writeString(file, """
                Experience_Values:
                  Taming:
                    Animal_Taming:
                      Wolf: nope
                """);
        assertThrows(IllegalArgumentException.class, () -> TamingXpTable.load(file));
    }
}
