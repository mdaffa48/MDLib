package com.muhammaddaffa.mdlib.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Placeholder {

    private final Map<String, String> replacements = new HashMap<>();

    public Placeholder add(String original, String replacement) {
        this.replacements.put(original, replacement);
        return this;
    }

    public Placeholder add(String original, int replacement) {
        return add(original, replacement + "");
    }

    public Placeholder add(String original, double replacement) {
        return add(original, replacement + "");
    }

    public String translate(String message) {
        String translated = message;
        for (String original : this.replacements.keySet()) {
            translated = translated.replace(original, this.replacements.get(original));
        }
        return translated;
    }

    public List<String> translate(List<String> messages) {
        return messages.stream()
                .map(this::translate)
                .collect(Collectors.toList());
    }

}
