package com.muhammaddaffa.mdlib.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;

public class ColorComponent {

    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .hexCharacter('#')
            .character('&')
            .useUnusualXRepeatedCharacterHexFormat()
            .hexColors()
            .extractUrls()
            .build();

    /**
     * Converts a String to a Component with color codes and decorations
     * It also supports hex colors and URLs
     *
     * @param message The message to convert
     * @return The converted message
     */
    public static Component colorToComponent(String message) {
        return colorToComponent(message, null);
    }

    /**
     * Converts a String to a Component with color codes and decorations
     * It also supports hex colors and URLs
     *
     * @param message     The message to convert
     * @return The converted message
     */
    public static Component colorToComponent(String message, @Nullable PlaceholderComponent placeholders) {
        if (message == null) return Component.empty();

        Component component = LEGACY.deserialize(message).decoration(TextDecoration.ITALIC, false);;

        if (placeholders != null) {
            component = placeholders.translate(component);
        }

        return component;
    }

}
