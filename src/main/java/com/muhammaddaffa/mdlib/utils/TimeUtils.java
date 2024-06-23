package com.muhammaddaffa.mdlib.utils;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private static final long TICK_MS = 50;
    private static final long SECOND_MS = 1000;
    private static final long MINUTE_MS = SECOND_MS * 60;
    private static final long HOUR_MS = MINUTE_MS * 60;
    private static final long DAY_MS = HOUR_MS * 24;
    private static final long YEAR_MS = DAY_MS * 365;

    private static final int DAYS_IN_SECOND = 86400;

    public static String APPEND_DAYS = "d";
    public static String APPEND_HOURS = "h";
    public static String APPEND_MINUTES = "m";
    public static String APPEND_SECONDS = "s";
    public static boolean SPACE_AFTER_APPEND = true;

    private static final Map<String, Long> unitMultipliers = new HashMap<>();

    private static void addTimeMultiplier(long multiplier, String... keys) {
        for(String key : keys) {
            unitMultipliers.put(key, multiplier);
        }
    }

    static {
        addTimeMultiplier(1, "ms", "milli", "millis", "millisecond", "milliseconds");
        addTimeMultiplier(TICK_MS, "t", "tick", "ticks");
        addTimeMultiplier(SECOND_MS, "s", "sec", "secs", "second", "seconds");
        addTimeMultiplier(MINUTE_MS, "m", "min", "mins", "minute", "minutes");
        addTimeMultiplier(HOUR_MS, "h", "hour", "hours");
        addTimeMultiplier(DAY_MS, "d", "day", "days");
        addTimeMultiplier(YEAR_MS, "y", "year", "years");
    }

    private long milliseconds;

    public TimeUtils(long time, TimeUnit timeUnit) {
        this(TimeUnit.MILLISECONDS.convert(time, timeUnit));
    }

    private TimeUtils(long milliseconds) {
        if(milliseconds < 0) {
            throw new IllegalArgumentException("Number of milliseconds cannot be less than 0");
        }

        this.milliseconds = milliseconds;
    }

    public long toMilliseconds() {
        return this.milliseconds;
    }

    public double toTicks() {
        return this.milliseconds / (double) TICK_MS;
    }

    public double toSeconds() {
        return this.milliseconds / (double) SECOND_MS;
    }

    public double toMinutes() {
        return this.milliseconds / (double) MINUTE_MS;
    }

    public double toHours() {
        return this.milliseconds / (double) HOUR_MS;
    }

    public double toDays() {
        return this.milliseconds / (double) DAY_MS;
    }

    public double toYears() {
        return this.milliseconds / (double) YEAR_MS;
    }

    @Override
    public String toString() {
        StringBuilder timeString = new StringBuilder();

        long time = this.milliseconds;

        time = this.appendTime(time, YEAR_MS, "years", timeString);
        time = this.appendTime(time, DAY_MS, "days", timeString);
        time = this.appendTime(time, HOUR_MS, "hours", timeString);
        time = this.appendTime(time, MINUTE_MS, "minutes", timeString);
        time = this.appendTime(time, SECOND_MS, "seconds", timeString);

        if(time != 0) {
            timeString.append(", ").append(time).append(" ms");
        }

        if(timeString.isEmpty()) {
            return "0 seconds";
        }

        return timeString.substring(2);
    }

    private long appendTime(long time, long unitInMS, String name, StringBuilder builder) {
        long timeInUnits = (time - (time % unitInMS)) / unitInMS;

        if(timeInUnits > 0) {
            builder.append(", ").append(timeInUnits).append(' ').append(name);
        }

        return time - timeInUnits * unitInMS;
    }

    @Nullable
    public static TimeUtils fromString(String timeString) throws TimeParseException {
        if (timeString == null || timeString.isEmpty()) return null;

        long totalMilliseconds = 0;
        boolean readingNumber = true;

        StringBuilder number = new StringBuilder();
        StringBuilder unit = new StringBuilder();

        for(char c : timeString.toCharArray()) {
            if(c == ' ' || c == ',') {
                readingNumber = false;
                continue;
            }

            if(c == '.' || (c >='0' && c <= '9')) {
                if(!readingNumber) {
                    totalMilliseconds += parseTimeComponent(number.toString(), unit.toString());

                    number.setLength(0);
                    unit.setLength(0);

                    readingNumber = true;
                }

                number.append(c);
            } else {
                readingNumber = false;
                unit.append(c);
            }
        }

        if(readingNumber) {
            throw new TimeParseException("Number \"" + number + "\" not matched with unit at end of string");
        } else {
            totalMilliseconds += parseTimeComponent(number.toString(), unit.toString());
        }

        return new TimeUtils(totalMilliseconds);
    }

    private static double parseTimeComponent(String magnitudeString, String unit) throws TimeParseException {
        if(magnitudeString.isEmpty()) {
            throw new TimeParseException("Missing number for unit \"" + unit + "\"");
        }

        long magnitude;

        try {
            magnitude = Long.valueOf(magnitudeString);
        } catch(NumberFormatException e) {
            throw new TimeParseException("Unable to parse number \"" + magnitudeString + "\"", e);
        }

        unit = unit.toLowerCase();

        if(unit.length() > 3 && unit.substring(unit.length() - 3).equals("and")) {
            unit = unit.substring(0, unit.length() - 3);
        }

        Long unitMultiplier = unitMultipliers.get(unit);

        if(unitMultiplier == null) {
            throw new TimeParseException("Unknown time unit \"" + unit + "\"");
        }

        return magnitude * unitMultiplier;
    }

    public static String format(long remaining) {
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

    public static class TimeParseException extends RuntimeException {

        public TimeParseException(String reason) {
            super(reason);
        }

        public TimeParseException(String reason, Throwable cause) {
            super(reason, cause);
        }

    }

}
