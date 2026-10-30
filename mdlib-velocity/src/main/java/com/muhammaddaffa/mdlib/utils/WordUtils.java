package com.muhammaddaffa.mdlib.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

public class WordUtils {

    // Same pattern as Bukkit's ChatColor.stripColor
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    /**
     * Turns the first letter of a String to uppercase, while making the rest lowercase
     *
     * @param word String to change
     */
    public static String upperCaseFirstLetterOnly(final String word) {
        return upperCaseFirstLetter(word.toLowerCase(Locale.ROOT));
    }

    /**
     * Turns the first letter of a String to uppercase
     *
     * @param word String to change
     */
    public static String upperCaseFirstLetter(final String word) {
        if (word.isEmpty()) return word;
        if (word.length() == 1) return word.toUpperCase(Locale.ROOT);
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    /**
     * Converts a given String into a human-readable String, by replacing underscores with spaces, and making all words Uppercase. For example, "ARMOR_STAND" will return "Armor Stand"
     */
    public static String getNiceName(@NotNull final String string) {
        final String[] split = string.split("_");
        final Iterator<String> iterator = Arrays.stream(split).iterator();
        final StringBuilder builder = new StringBuilder();
        while (iterator.hasNext()) {
            builder.append(upperCaseFirstLetterOnly(iterator.next().toLowerCase(Locale.ROOT)));
            if (iterator.hasNext()) builder.append(" ");
        }
        return builder.toString();
    }

    /**
     * Builds a String from the given args and starting point
     *
     * @param args  args String Array
     * @param start Starting index to append strings together with
     * @return String The final String
     */
    public static String buildString(final String[] args, final int start) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            stringBuilder.append(args[i]).append(" ");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    public static int countChar(final String string, final char aChar) {
        int count = 0;
        char[] arr = string.toCharArray();
        for (int i = 0; i < string.length(); i++) {
            if (arr[i] == aChar) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the correct English genitive suffix for the given word. E.g., for "mfnalex" it will return "'s", for "Jesus" it will return "'"
     *
     * @param word The word to get the genitive suffix for
     * @return The genitive suffix
     */
    public static String getGenitiveSuffix(final String word) {
        final String strippedWord = STRIP_COLOR_PATTERN.matcher(word).replaceAll("");
        if (strippedWord.endsWith("s")) {
            return "'";
        }
        return "'s";
    }

}
