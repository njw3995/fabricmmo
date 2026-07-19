package io.github.njw3995.fabricmmo.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class FlatYamlConfigTest {
    @Test
    void parsesNestedScalarValuesAndQuotedComments() throws Exception {
        FlatYamlConfig config = FlatYamlConfig.parse(new StringReader("""
                Root:
                  Enabled: true
                  Count: 8
                  Ratio: 1.5
                  Label: "value # retained"
                """), "test");

        assertTrue(config.requiredBoolean("Root.Enabled"));
        assertEquals(8, config.requiredInt("Root.Count"));
        assertEquals(1.5D, config.requiredDouble("Root.Ratio"));
        assertEquals("value # retained", config.requiredString("Root.Label"));
        assertFalse(config.bool("Root.Missing", false));
    }

    @Test
    void rejectsInvalidBoolean() throws Exception {
        FlatYamlConfig config = FlatYamlConfig.parse(
                new StringReader("Enabled: perhaps\n"), "test");
        assertThrows(IllegalArgumentException.class,
                () -> config.requiredBoolean("Enabled"));
    }

    @Test
    void preservesScalarOrderAndIgnoresYamlSequenceMembers() throws Exception {
        FlatYamlConfig config = FlatYamlConfig.parse(new StringReader("""
                Root:
                  First: 1
                  List:
                    - alpha
                    - beta
                  Second: 2
                """), "test");

        assertEquals(java.util.List.of("Root.First", "Root.Second"),
                java.util.List.copyOf(config.valuesWithPrefix("Root.").keySet()));
    }
}
