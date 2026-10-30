package com.muhammaddaffa.mdlib.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Colors {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?:&#|(?<!<)#)([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9A-FK-ORa-fk-or])");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");

    // Adventure format
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .hexCharacter('#')
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .useUnusualXRepeatedCharacterHexFormat()
            .extractUrls()
            .build();

    // MiniMessage
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Map<String, TagResolver> TAG_RESOLVERS = new LinkedHashMap<>();
    private static volatile TagResolver customTags = TagResolver.empty();

    private Colors() {
    }

    public static synchronized void registerTagResolver(String id, TagResolver resolver) {
        TAG_RESOLVERS.put(Objects.requireNonNull(id), Objects.requireNonNull(resolver));
        customTags = TagResolver.resolver(TAG_RESOLVERS.values());
    }

    public static synchronized void unregisterTagResolver(String id) {
        TAG_RESOLVERS.remove(id);
        customTags = TagResolver.resolver(TAG_RESOLVERS.values());
    }

    public static synchronized void clearTagResolvers() {
        TAG_RESOLVERS.clear();
        customTags = TagResolver.empty();
    }

    public static List<String> color(List<String> messages) {
        return messages.stream().map(Colors::color).collect(Collectors.toList());
    }

    /**
     * Legacy string output. Cannot retain click or hover events. Use
     * {@link #component(String)} when the message needs the full format range.
     */
    public static String color(String message) {
        if (message == null) {
            return null;
        }
        return LEGACY_COMPONENT_SERIALIZER.serialize(component(message));
    }

    public static List<Component> component(List<String> messages) {
        return messages.stream().map(Colors::component).collect(Collectors.toList());
    }

    public static Component component(String message) {
        return component(message, TagResolver.empty());
    }

    public static Component component(String message, TagResolver resolver) {
        if (message == null) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(message), customTags, resolver);
    }

    private static String legacyToMiniMessage(String message) {
        StringBuilder result = new StringBuilder(message.length() + 16);
        int lastEnd = 0;
        for (int index = 0; index < message.length(); index++) {
            if (message.charAt(index) == '\\') {
                index++;
                continue;
            }
            if (message.charAt(index) != '<') {
                continue;
            }
            int end = tagEnd(message, index + 1);
            if (end < 0) {
                continue;
            }
            result.append(convertLegacy(message.substring(lastEnd, index)));
            result.append(message, index, end + 1);
            lastEnd = end + 1;
            index = end;
        }

        result.append(convertLegacy(message.substring(lastEnd)));
        return result.toString();
    }

    private static int tagEnd(String message, int start) {
        char quote = 0;
        for (int index = start; index < message.length(); index++) {
            char current = message.charAt(index);
            if (quote != 0) {
                if (current == '\\') {
                    index++;
                } else if (current == quote) {
                    quote = 0;
                }
            } else if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '>') {
                return index;
            } else if (current == '<') {
                return -1;
            }
        }
        return -1;
    }

    private static String convertLegacy(String message) {
        message = message.replace('§', '&');
        if (message.isEmpty()) {
            return message;
        }

        Matcher legacyHexMatcher = LEGACY_HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 16);

        while (legacyHexMatcher.find()) {
            String legacyHex = legacyHexMatcher.group();
            String hex = legacyHex
                    .replace("&x", "")
                    .replace("&", "");

            legacyHexMatcher.appendReplacement(buffer, Matcher.quoteReplacement("<reset><#" + hex + ">"));
        }

        message = legacyHexMatcher.appendTail(buffer).toString();

        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        buffer.setLength(0);

        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(buffer, Matcher.quoteReplacement("<reset><#" + hexMatcher.group(1) + ">"));
        }

        Matcher legacyMatcher = LEGACY_COLOR_PATTERN.matcher(hexMatcher.appendTail(buffer).toString());
        buffer.setLength(0);

        while (legacyMatcher.find()) {
            legacyMatcher.appendReplacement(buffer, legacyCodeToMiniMessage(legacyMatcher.group(1).charAt(0)));
        }

        return legacyMatcher.appendTail(buffer).toString();
    }

    private static String legacyCodeToMiniMessage(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<reset><black>";
            case '1' -> "<reset><dark_blue>";
            case '2' -> "<reset><dark_green>";
            case '3' -> "<reset><dark_aqua>";
            case '4' -> "<reset><dark_red>";
            case '5' -> "<reset><dark_purple>";
            case '6' -> "<reset><gold>";
            case '7' -> "<reset><gray>";
            case '8' -> "<reset><dark_gray>";
            case '9' -> "<reset><blue>";
            case 'a' -> "<reset><green>";
            case 'b' -> "<reset><aqua>";
            case 'c' -> "<reset><red>";
            case 'd' -> "<reset><light_purple>";
            case 'e' -> "<reset><yellow>";
            case 'f' -> "<reset><white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }

}
