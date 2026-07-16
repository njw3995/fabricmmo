package io.github.njw3995.fabricmmo.tools.upstreamdiff;

import java.nio.file.Path;
import java.util.Comparator;

public final class UpstreamDiffMain {
    private UpstreamDiffMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: upstream-diff <repository> <from-revision> <to-revision>");
            System.exit(2);
        }
        var changes = new UpstreamDiffAnalyzer().analyze(Path.of(args[0]), args[1], args[2]);
        changes.stream()
                .sorted(Comparator.comparing(ChangedFile::category).thenComparing(ChangedFile::path))
                .forEach(change -> System.out.printf("%s\t%s\t%s%n",
                        change.changeType(), change.category(), change.path()));
    }
}
