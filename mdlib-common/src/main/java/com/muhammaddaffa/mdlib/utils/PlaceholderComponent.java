package com.muhammaddaffa.mdlib.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

import java.util.*;

public class PlaceholderComponent {

    private final Map<String, String> replacements = new LinkedHashMap<>();

    /**
     * Add a placeholder to the list of replacements
     *
     * @param original The original text
     * @param replaced The text to replace the original with
     * @return The PlaceholderComponent instance
     */
    public PlaceholderComponent add(String original, int replaced) {
        return add(original, Integer.toString(replaced));
    }

    /**
     * Add a placeholder to the list of replacements
     *
     * @param original The original text
     * @param replaced The text to replace the original with
     * @return The PlaceholderComponent instance
     */
    public PlaceholderComponent add(String original, double replaced) {
        return add(original, Double.toString(replaced));
    }

    /**
     * Add a placeholder to the list of replacements
     *
     * @param original The original text
     * @param replaced The text to replace the original with
     * @return The PlaceholderComponent instance
     */
    public PlaceholderComponent add(String original, String replaced) {
        if (original == null || replaced == null) {
            return this;
        }
        // Check if the original contains color codes and remove them
        this.replacements.put(original, replaced);
        return this;
    }

    /**
     * Replace the placeholders in the list of components
     *
     * @param components The list of components to replace the placeholders in
     * @return The list of components with the placeholders replaced
     */
    public List<Component> translate(List<Component> components) {
        List<Component> translated = new ArrayList<>();
        for (Component component : components) {
            translated.add(translate(component));
        }
        return translated;
    }

    /**
     * Replace the placeholders in the component
     *
     * @param component The component to replace the placeholders in
     * @return The component with the placeholders replaced
     */
    public Component translate(Component component) {
        if (component == null || this.replacements.isEmpty()) {
            return component;
        }

        // Replace the placeholders
        Component result = component;

        // Replace the placeholders
        for (Map.Entry<String, String> entry : this.replacements.entrySet()) {
            // Get the original and replacement
            String original = entry.getKey();
            String replacement = entry.getValue();

            // Create the replacement config
            TextReplacementConfig config = TextReplacementConfig.builder()
                    .matchLiteral(original)
                    .replacement(replacement)
                    .build();

            // Replace the text
            result = result.replaceText(config);

        }
        return result;
    }

}
