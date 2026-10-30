package com.muhammaddaffa.mdlib.core;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LogSink {

    private static final Logger FALLBACK = Logger.getLogger("MDLib");
    private static volatile BiConsumer<Level, String> output;

    private LogSink() {
    }

    /**
     * Sends every MDLib Logger message to the given output. Each platform's MDLib.onEnable installs its plugin logger here.
     */
    public static void install(BiConsumer<Level, String> output) {
        LogSink.output = Objects.requireNonNull(output, "output");
    }

    public static void uninstall() {
        output = null;
    }

    /**
     * Logs through the installed output, or through a plain "MDLib" logger when no platform is enabled.
     */
    public static void log(Level level, String message) {
        BiConsumer<Level, String> current = output;
        if (current != null) {
            current.accept(level, message);
        } else {
            FALLBACK.log(level, message);
        }
    }

}
