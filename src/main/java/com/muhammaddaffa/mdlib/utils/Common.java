package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Common {

    private static final DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###,###.##");

    public static long parseTime(String string) {
        // checks if the string is either null and empty and if so returns 0
        if (string == null || string.isEmpty())
            return 0L;
        // this replaces the regex for 0-9 and the other characters
        string = string.replaceAll("[^0-9smhdw]", "");
        // checks if the new string is empty since we removed some characters
        if (string.isEmpty())
            return 0L;
        // Check if string contains "w"
        if (string.contains("w")) {
            // Replace all non numbers with nothing
            string = string.replaceAll("[^0-9]", "");
            // Another empty check
            if (string.isEmpty())
                return 0L;
            // If it has a number we change the number value to days by
            // multiplying by 7 then we can change it to seconds
            return TimeUnit.DAYS.toSeconds(Long.parseLong(string) * 7);
        }
        // First we check for days using "d"
        TimeUnit unit = string.contains("d") ? TimeUnit.DAYS
                // If the string contains "h" it goes for hours
                : string.contains("h") ? TimeUnit.HOURS
                // If the string contains "m" it goes for minutes
                : string.contains("m") ? TimeUnit.MINUTES
                // Finally, if none match we go with seconds
                : TimeUnit.SECONDS;
        // Next we replace all the non-numbers with nothing so it can match a
        // number
        string = string.replaceAll("[^0-9]", "");
        // Another empty check to make sure something is there
        if (string.isEmpty())
            return 0L;
        // Then we return the string as a long in seconds using the unit
        // selected earlier
        return unit.toSeconds(Long.parseLong(string));
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

    public static void configBroadcast(String configName, String path) {
        configBroadcast(configName, path, null);
    }

    public static void configBroadcast(String configName, String path, @Nullable Placeholder placeholder) {
        Config config = Config.getConfig(configName);
        if (config == null) {
            Logger.warning("Trying to send config broadcast from invalid config (" + configName + ") with path '" + path + "'");
            return;
        }
        configBroadcast(config, path, placeholder);
    }

    public static void configBroadcast(@NotNull Config config, String path) {
        configBroadcast(config, path, null);
    }

    public static void configBroadcast(@NotNull Config config, String path, @Nullable Placeholder placeholder) {
        FileConfiguration fileConfiguration = config.getConfig();
        if (fileConfiguration.isList(path)) {
            for (String message : fileConfiguration.getStringList(path)) {
                broadcast(message, placeholder);
            }
        } else {
            broadcast(fileConfiguration.getString(path), placeholder);
        }
    }

    public static void broadcast(String message) {
        broadcast(message, null);
    }

    public static void broadcast(String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        Bukkit.broadcastMessage(color(message));
    }

    public static void actionBar(Player player, String message) {
        actionBar(player, message, null);
    }

    public static void actionBar(Player player, String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        // send the action bar message
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Common.color(message)));
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
        player.sendTitle(Common.color(title), Common.color(subTitle), fadeIn, stay, fadeOut);
    }

    public static String digits(Object o) {
        return decimalFormat.format(o);
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
        player.getInventory().addItem(stack).forEach((integer, item) -> {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        });
    }

    public static void addInventoryItem(Player player, List<ItemStack> items) {
        for (ItemStack stack : items) {
            player.getInventory().addItem(stack).forEach((integer, item) -> {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            });
        }
    }

    public static void configMessage(String configName, CommandSender sender, String path) {
        configMessage(configName, sender, path, null);
    }

    public static void configMessage(String configName, CommandSender sender, String path, @Nullable Placeholder placeholder) {
        Config config = Config.getConfig(configName);
        if (config == null) {
            Logger.warning("Trying to send config message from invalid config (" + configName + ") with path '" + path + "'");
            return;
        }
        configMessage(config, sender, path, placeholder);
    }

    public static void configMessage(@NotNull Config config, CommandSender sender, String path) {
        configMessage(config, sender, path, null);
    }

    public static void configMessage(@NotNull Config config, CommandSender sender, String path, @Nullable Placeholder placeholder) {
        FileConfiguration fileConfiguration = config.getConfig();
        if (fileConfiguration.isList(path)) {
            for (String message : fileConfiguration.getStringList(path)) {
                sendMessage(sender, message, placeholder);
            }
        } else {
            sendMessage(sender, fileConfiguration.getString(path), placeholder);
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
        sender.sendMessage(color(message));
    }

    public static List<String> color(List<String> messages) {
        return messages.stream().map(Common::color).collect(Collectors.toList());
    }

    public static String color(String message) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&" + c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String papi(Player player, String message) {
        if (!MDLib.usingPlaceholderAPI()) {
            return message;
        }
        return PlaceholderAPI.setPlaceholders(player, message);
    }

}
