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

    @Deprecated
    public static void configBroadcast(String configName, String path) {
        configBroadcast(configName, path, null);
    }

    @Deprecated
    public static void configBroadcast(String configName, String path, @Nullable Placeholder placeholder) {
        Config config = Config.getConfig(configName);
        if (config == null) {
            Logger.warning("Trying to send config broadcast from invalid config (" + configName + ") with path '" + path + "'");
            return;
        }
        configBroadcast(config, path, placeholder);
    }

    @Deprecated
    public static void configBroadcast(@NotNull Config config, String path) {
        configBroadcast(config, path, null);
    }

    @Deprecated
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

    @Deprecated
    public static void configMessage(String configName, CommandSender sender, String path) {
        configMessage(configName, sender, path, null);
    }

    @Deprecated
    public static void configMessage(String configName, CommandSender sender, String path, @Nullable Placeholder placeholder) {
        Config config = Config.getConfig(configName);
        if (config == null) {
            Logger.warning("Trying to send config message from invalid config (" + configName + ") with path '" + path + "'");
            return;
        }
        configMessage(config, sender, path, placeholder);
    }

    @Deprecated
    public static void configMessage(@NotNull Config config, CommandSender sender, String path) {
        configMessage(config, sender, path, null);
    }

    @Deprecated
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
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return message;
        }
        return PlaceholderAPI.setPlaceholders(player, message);
    }

}
