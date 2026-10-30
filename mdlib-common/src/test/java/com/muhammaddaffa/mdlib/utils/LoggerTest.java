package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.core.LogSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTest {

    @AfterEach
    void uninstall() {
        LogSink.uninstall();
    }

    @Test
    void everyMessageGoesToTheInstalledOutputAtItsLevel() {
        List<String> lines = new ArrayList<>();
        LogSink.install((level, message) -> lines.add(level + " " + message));

        Logger.info("a", "b");
        Logger.warning("c");
        Logger.severe("d");
        Logger.finest("e");

        assertEquals(List.of("INFO a", "INFO b", "WARNING c", "SEVERE d", "FINEST e"), lines);
    }

    @Test
    void loggingWithoutAPlatformGoesToTheMDLibLogger() {
        java.util.logging.Logger fallback = java.util.logging.Logger.getLogger("MDLib");
        List<String> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { records.add(record.getLevel() + " " + record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        fallback.addHandler(handler);
        try {
            Logger.info("before onEnable");
            Logger.severe("broken");
        } finally {
            fallback.removeHandler(handler);
        }

        assertEquals(List.of("INFO before onEnable", "SEVERE broken"), records);
        assertThrows(NullPointerException.class, () -> LogSink.install(null));
    }

    @Test
    void uninstallingStopsSendingToTheOldOutput() {
        List<String> lines = new ArrayList<>();
        LogSink.install((level, message) -> lines.add(message));
        LogSink.uninstall();

        Logger.info("after shutdown");

        assertTrue(lines.isEmpty());
    }
}
