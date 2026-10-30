package com.muhammaddaffa.mdlib.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Placeholder {

    private final Map<String, String> replacements = new HashMap<>();

    public Placeholder add(String original, String replacement) {
        if (original == null || replacement == null) {
           return this;
        }
        this.replacements.put(original, replacement);
        return this;
    }

    public Placeholder add(String original, int replacement) {
        return add(original, Integer.toString(replacement));
    }

    public Placeholder add(String original, double replacement) {
        return add(original, Double.toString(replacement));
    }

    public String translate(String message) {
        if (message == null) {
            return null;
        }
        String translated = message;
        for (Map.Entry<String, String> entry : this.replacements.entrySet()) {
            String original = entry.getKey();
            String replacement = entry.getValue();
            // Check for null
            if (original != null && replacement != null) {
                translated = translated.replace(original, replacement);
            }
        }
        for (String original : this.replacements.keySet()) {
            String result = this.replacements.get(original);
            if (result != null) translated = translated.replace(original, result);
        }
        return translated;
    }

    public List<String> translate(List<String> messages) {
        return messages.stream()
                .map(this::translate)
                .collect(Collectors.toList());
    }

}
