package io.github.njw3995.fabricmmo.tools.upstreamdiff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UpstreamDiffAnalyzerTest {
    @Test
    void classifiesConfigAndPersistenceChanges() {
        UpstreamDiffAnalyzer analyzer = new UpstreamDiffAnalyzer();
        assertEquals(ChangedFile.Category.CONFIG,
                analyzer.parse("M\tsrc/main/resources/experience.yml").category());
        assertEquals(ChangedFile.Category.PERSISTENCE,
                analyzer.parse("M\tsrc/main/java/example/SQLDatabaseManager.java").category());
    }
}
