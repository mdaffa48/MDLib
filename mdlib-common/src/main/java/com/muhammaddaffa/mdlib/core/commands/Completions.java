package com.muhammaddaffa.mdlib.core.commands;

import java.util.ArrayList;
import java.util.List;

public final class Completions {

    private Completions() {
    }

    /**
     * The entries of {@code originals} that start with {@code token}, ignoring case, in their original order.
     * Matches Bukkit's StringUtil.copyPartialMatches.
     */
    public static List<String> partialMatches(String token, Iterable<String> originals) {
        List<String> out = new ArrayList<>();
        for (String string : originals) {
            if (string.length() >= token.length() && string.regionMatches(true, 0, token, 0, token.length())) {
                out.add(string);
            }
        }
        return out;
    }

}
