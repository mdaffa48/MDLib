package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.core.LogSink;

import java.util.logging.Level;

public class Logger {

    public static void info(String... messages) {
        for (String message : messages) {
            LogSink.log(Level.INFO, message);
        }
    }

    public static void warning(String... messages) {
        for (String message : messages) {
            LogSink.log(Level.WARNING, message);
        }
    }

    public static void severe(String... messages) {
        for (String message : messages) {
            LogSink.log(Level.SEVERE, message);
        }
    }

    public static void finest(String... messages) {
        for (String message : messages) {
            LogSink.log(Level.FINEST, message);
        }
    }

}
