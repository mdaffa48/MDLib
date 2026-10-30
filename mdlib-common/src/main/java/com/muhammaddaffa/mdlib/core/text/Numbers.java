package com.muhammaddaffa.mdlib.core.text;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BinaryOperator;

public final class Numbers {

    // DecimalFormat is not thread-safe, and proxies run handlers on many threads at once.
    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT =
            ThreadLocal.withInitial(() -> new DecimalFormat("###,###,###,###,###.##"));

    private Numbers() {
    }

    public static double getRandomNumberBetween(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(max - min) + min;
    }

    public static int getRandomNumberBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(max - min) + min;
    }

    public static boolean isValid(List<?> list, int index) {
        try {
            list.get(index);
            return true;
        } catch (IndexOutOfBoundsException ex) {
            return false;
        }
    }

    public static String digits(Object o) {
        return DECIMAL_FORMAT.get().format(o);
    }

    public static String format(double number) {
        return format(number, (path, defaultSuffix) -> defaultSuffix);
    }

    /**
     * Formats with a short suffix such as "K" or "M". {@code notation} receives the config path
     * "currency-notation.&lt;name&gt;" and the default suffix, and returns the suffix to use.
     */
    public static String format(double number, BinaryOperator<String> notation) {
        DecimalFormat decimalFormat = DECIMAL_FORMAT.get();
        if (number >= 1_000_000_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000_000_000.0) + notation.apply("currency-notation.septillion", "Sp");
        } else if (number >= 1_000_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000_000.0) + notation.apply("currency-notation.sextillion", "Sx");
        } else if (number >= 1_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000.0) + notation.apply("currency-notation.quintillion", "Qi");
        } else if (number >= 1_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000.0) + notation.apply("currency-notation.quadrillion", "Qa");
        } else if (number >= 1_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000.0) + notation.apply("currency-notation.trillion", "T");
        } else if (number >= 1_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000.0) + notation.apply("currency-notation.billion", "B");
        } else if (number >= 1_000_000.0) {
            return decimalFormat.format(number / 1_000_000.0) + notation.apply("currency-notation.million", "M");
        } else if (number >= 1_000.0) {
            return decimalFormat.format(number / 1_000.0) + notation.apply("currency-notation.thousand", "K");
        } else {
            return decimalFormat.format(number);
        }
    }

    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

}
