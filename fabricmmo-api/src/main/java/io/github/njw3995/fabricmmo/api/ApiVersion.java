package io.github.njw3995.fabricmmo.api;

/** Version helpers for addons that require a particular public API capability. */
public final class ApiVersion {
    public static final int MAJOR = 1;
    public static final int MINOR = 4;
    public static final String STRING = MAJOR + "." + MINOR;

    private ApiVersion() {
    }

    /** Returns whether {@code actual} is the same major and at least the requested minor. */
    public static boolean isAtLeast(String actual, int requiredMajor, int requiredMinor) {
        int[] parsed = parse(actual);
        return parsed[0] == requiredMajor && parsed[1] >= requiredMinor;
    }

    /** Throws a descriptive exception when an addon cannot run on the supplied API version. */
    public static void requireAtLeast(
            String actual,
            int requiredMajor,
            int requiredMinor,
            String consumer) {
        if (!isAtLeast(actual, requiredMajor, requiredMinor)) {
            throw new IllegalStateException(
                    consumer + " requires FabricMMO API " + requiredMajor + '.' + requiredMinor
                            + " or newer within major " + requiredMajor + "; found " + actual);
        }
    }

    private static int[] parse(String value) {
        if (value == null) throw new NullPointerException("value");
        String[] parts = value.trim().split("\\.", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Unrecognized FabricMMO API version: " + value);
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (major < 0 || minor < 0) throw new NumberFormatException("negative component");
            return new int[] {major, minor};
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Unrecognized FabricMMO API version: " + value, exception);
        }
    }
}
