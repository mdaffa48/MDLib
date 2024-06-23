package com.muhammaddaffa.mdlib.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

public class WordUtils {

    /**
     * Turns Material names into a nicer name. E.g., DIAMOND_PICKAXE will return "Diamond Pickaxe"
     *
     * @param mat The Material
     * @return Human-readable name
     */
    public static String getNiceMaterialName(final Material mat) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<String> iterator = Arrays.stream(mat.name().split("_")).iterator();
        while (iterator.hasNext()) {
            builder.append(upperCaseFirstLetterOnly(iterator.next()));
            if (iterator.hasNext()) builder.append(" ");
        }
        return builder.toString();
    }

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
     * Converts a NamespacedKey into a human-readable name, ignoring the namespace. For example, "minecraft:warm_ocean" will return "Warm Ocean"
     */
    public static String getNiceName(@NotNull final NamespacedKey key) {
        return getNiceName(key.getKey());
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
        final String strippedWord = ChatColor.stripColor(word);
        if (strippedWord.endsWith("s")) {
            return "'";
        }
        return "'s";
    }

}
