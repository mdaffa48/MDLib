package com.muhammaddaffa.mdlib.utils;

public class TimeFormat {

    private static final int DAYS_IN_SECOND = 86400;

    public static String APPEND_DAYS = "d";
    public static String APPEND_HOURS = "h";
    public static String APPEND_MINUTES = "m";
    public static String APPEND_SECONDS = "s";
    public static boolean SPACE_AFTER_APPEND = true;

    public static String parse(long remaining) {
        int days = toDays(remaining);
        int hours = toHours(remaining);
        int minutes = toMinutes(remaining);
        int seconds = toSeconds(remaining);
        StringBuilder builder = new StringBuilder();
        // Add days if it's not 0
        if (days != 0) {
            builder.append(days).append(APPEND_DAYS);
            if (hours != 0 && SPACE_AFTER_APPEND) {
                builder.append(" ");
            }
        }
        // Add hours if it's not 0
        if (hours != 0) {
            builder.append(hours).append(APPEND_HOURS);
            if (minutes != 0 && SPACE_AFTER_APPEND) {
                builder.append(" ");
            }
        }
        // Add minutes if it's not 0
        if (minutes != 0) {
            builder.append(minutes).append(APPEND_MINUTES);
            if (seconds != 0 && SPACE_AFTER_APPEND) {
                builder.append(" ");
            }
        }
        // Add seconds if it's not 0
        if (seconds != 0) {
            builder.append(seconds).append(APPEND_SECONDS);
        }
        return builder.toString();
    }

    private static int toDays(long remaining) {
        return (int) (remaining / DAYS_IN_SECOND);
    }

    private static int toHours(long remaining) {
        return (int) ((remaining % DAYS_IN_SECOND) / 3600);
    }

    private static int toMinutes(long remaining) {
        return (int) (((remaining % DAYS_IN_SECOND) % 3600) / 60);
    }

    private static int toSeconds(long remaining) {
        return (int) (((remaining % DAYS_IN_SECOND) % 3600) % 60);
    }

}
