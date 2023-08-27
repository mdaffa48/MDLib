package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;

public class Logger {

    public static void info(String... messages) {
        for (String message : messages) {
            MDLib.getInstance().getLogger().info(message);
        }
    }

    public static void warning(String... messages) {
        for (String message : messages) {
            MDLib.getInstance().getLogger().warning(message);
        }
    }

    public static void severe(String... messages) {
        for (String message : messages) {
            MDLib.getInstance().getLogger().severe(message);
        }
    }

    public static void finest(String... messages) {
        for (String message : messages) {
            MDLib.getInstance().getLogger().finest(message);
        }
    }
    
}
