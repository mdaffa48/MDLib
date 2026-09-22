package com.muhammaddaffa.mdlib.utils;

import com.cryptomorin.xseries.XSound;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Common {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?:&#|(?<!<)#)([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9A-FK-ORa-fk-or])");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private static final Pattern MINI_MESSAGE_TAG_PATTERN = Pattern.compile("</?[A-Za-z#][^<>]*>");
    private static final DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###,###.##");

    // Adventure format
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .hexCharacter('#')
            .character(ChatColor.COLOR_CHAR)
            .useUnusualXRepeatedCharacterHexFormat()
            .extractUrls()
            .build();

    // MiniMessage
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

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

    public static void playSound(Player player, Sound sound) {
        playSound(player, XSound.of(sound));
    }

    public static void playSound(Player player, String sound) {
        Optional<XSound> optional = XSound.of(sound);
        optional.ifPresent(xSound -> playSound(player, xSound));
    }

    public static void playSound(Player player, XSound sound) {
        sound.play(player, 1.0f, 1.0f);
    }

    public static void broadcast(String message) {
        broadcast(message, null);
    }

    public static void broadcast(String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        Bukkit.broadcast(component(message));
    }

    public static void actionBar(Player player, String message) {
        actionBar(player, message, null);
    }

    public static void actionBar(Player player, String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        // send the action bar message
        player.sendActionBar(component(message));
    }

    public static void sendTitle(Player player, String title, String subTitle) {
        sendTitle(player, title, subTitle, null);
    }

    public static void sendTitle(Player player, String title, String subTitle, @Nullable Placeholder placeholder) {
        sendTitle(player, title, subTitle, 20, 40, 20, placeholder);
    }

    public static void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        sendTitle(player, title, subTitle, fadeIn, stay, fadeOut, null);
    }

    public static void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            title = placeholder.translate(title);
            subTitle = placeholder.translate(subTitle);
        }
        player.showTitle(Title.title(component(title), component(subTitle),
                Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut))));
    }

    public static String digits(Object o) {
        return decimalFormat.format(o);
    }

    public static String format(FileConfiguration config, double number) {
        if (number >= 1_000_000_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000_000_000.0) + config.getString("currency-notation.septillion", "Sp");
        } else if (number >= 1_000_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000_000.0) + config.getString("currency-notation.sextillion", "Sx");
        } else if (number >= 1_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000.0) + config.getString("currency-notation.quintillion", "Qi");
        } else if (number >= 1_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000.0) + config.getString("currency-notation.quadrillion", "Qa");
        } else if (number >= 1_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000.0) + config.getString("currency-notation.trillion", "T");
        } else if (number >= 1_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000.0) + config.getString("currency-notation.billion", "B");
        } else if (number >= 1_000_000.0) {
            return decimalFormat.format(number / 1_000_000.0) + config.getString("currency-notation.million", "M");
        } else if (number >= 1_000.0) {
            return decimalFormat.format(number / 1_000.0) + config.getString("currency-notation.thousand", "K");
        } else {
            return decimalFormat.format(number);
        }
    }

    public static String format(double number) {
        if (number >= 1_000_000_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000_000_000.0) + "Sp";
        } else if (number >= 1_000_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000_000.0) + "Sx";
        } else if (number >= 1_000_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000_000.0) + "Qi";
        } else if (number >= 1_000_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000_000.0) + "Qa";
        } else if (number >= 1_000_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000_000.0) + "T";
        } else if (number >= 1_000_000_000.0) {
            return decimalFormat.format(number / 1_000_000_000.0) + "B";
        } else if (number >= 1_000_000.0) {
            return decimalFormat.format(number / 1_000_000.0) + "M";
        } else if (number >= 1_000.0) {
            return decimalFormat.format(number / 1_000.0) + "K";
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

    public static void addInventoryItem(Player player, ItemStack stack) {
        int amount = stack.getAmount();
        Material material = stack.getType();
        int maxStackSize = material.getMaxStackSize();

        // Clone the original stack to retain ItemMeta and other properties
        ItemStack originalStack = stack.clone();

        Map<Integer, ItemStack> leftovers = new HashMap<>();

        while (amount > 0) {
            // Determine the number of items for this part of the stack
            int stackAmount = Math.min(amount, maxStackSize);
            originalStack.setAmount(stackAmount);

            // Try to add the items to the player's inventory
            Map<Integer, ItemStack> left = player.getInventory().addItem(originalStack);
            if (!left.isEmpty()) {
                leftovers.putAll(left);
            }

            amount -= stackAmount;
        }

        // Drop any leftovers
        leftovers.values().forEach(item -> {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        });
    }

    public static void addInventoryItem(Player player, List<ItemStack> items) {
        for (ItemStack stack : items) {
            addInventoryItem(player, stack);
        }
    }

    public static void sendMessage(CommandSender sender, List<String> messages) {
        sendMessage(sender, messages, null);
    }

    public static void sendMessage(CommandSender sender, List<String> messages, Placeholder placeholder) {
        messages.forEach(message -> sendMessage(sender, message, placeholder));
    }

    public static void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, message, null);
    }

    public static void sendMessage(CommandSender sender, String message, Placeholder placeholder) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (sender instanceof Player player) {
            message = papi(player, message);
        }
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        // Check if message starts with 'actionbar;'
        if (sender instanceof Player player && message.startsWith("actionbar;")) {
            Common.actionBar(player, message.replace("actionbar;", ""));
        } else {
            sender.sendMessage(component(message.replace("actionbar;", "")));
        }
    }

    public static List<String> color(List<String> messages) {
        return messages.stream().map(Common::color).collect(Collectors.toList());
    }

    /**
     * Legacy string output. Cannot carry object components such as
     * {@code <sprite:...>} — those flatten to plain text. Use
     * {@link #component(String)} when the message needs the full format range.
     */
    public static String color(String message) {
        if (message == null) {
            return null;
        }
        return LEGACY_COMPONENT_SERIALIZER.serialize(component(message));
    }

    public static List<Component> component(List<String> messages) {
        return messages.stream().map(Common::component).collect(Collectors.toList());
    }

    public static Component component(String message) {
        if (message == null) {
            return Component.empty();
        }

        if (message.indexOf('§') >= 0) {
            message = message.replace('§', '&');
        }

        return MINI_MESSAGE.deserialize(legacyToMiniMessage(message));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(ticks * 50L);
    }


    private static String legacyToMiniMessage(String message) {
        Matcher tagMatcher = MINI_MESSAGE_TAG_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder(message.length() + 16);
        int lastEnd = 0;

        while (tagMatcher.find()) {
            result.append(convertLegacy(message.substring(lastEnd, tagMatcher.start())));
            result.append(tagMatcher.group());
            lastEnd = tagMatcher.end();
        }

        result.append(convertLegacy(message.substring(lastEnd)));
        return result.toString();
    }

    private static String convertLegacy(String message) {
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

    public static String papi(Player player, String message) {
        try {
            return PlaceholderAPI.setPlaceholders(player, message);
        } catch (Exception ex) {
            return message;
        }
    }

}
