package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.core.text.Colors;
import com.muhammaddaffa.mdlib.core.text.Numbers;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.time.Duration;
import java.util.List;

public class Common {

    public static synchronized void registerTagResolver(String id, TagResolver resolver) {
        Colors.registerTagResolver(id, resolver);
    }

    public static synchronized void unregisterTagResolver(String id) {
        Colors.unregisterTagResolver(id);
    }

    public static synchronized void clearTagResolvers() {
        Colors.clearTagResolvers();
    }

    public static double getRandomNumberBetween(double min, double max) {
        return Numbers.getRandomNumberBetween(min, max);
    }

    public static int getRandomNumberBetween(int min, int max) {
        return Numbers.getRandomNumberBetween(min, max);
    }

    public static boolean isValid(List<?> list, int index) {
        return Numbers.isValid(list, index);
    }

    public static void broadcast(String message) {
        broadcast(message, null);
    }

    /**
     * Sends the message to every online player and to the console.
     */
    public static void broadcast(String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        Component component = component(message);
        ProxyServer server = MDLib.server();
        for (Player player : server.getAllPlayers()) {
            player.sendMessage(component);
        }
        server.getConsoleCommandSource().sendMessage(component);
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
        return Numbers.digits(o);
    }

    public static String format(CommentedConfigurationNode config, double number) {
        return Numbers.format(number, (path, defaultSuffix) -> config.node((Object[]) path.split("\\.")).getString(defaultSuffix));
    }

    public static String format(double number) {
        return Numbers.format(number);
    }

    public static boolean isDouble(String s) {
        return Numbers.isDouble(s);
    }

    public static boolean isInt(String s) {
        return Numbers.isInt(s);
    }

    public static void sendMessage(CommandSource sender, List<String> messages) {
        sendMessage(sender, messages, null);
    }

    public static void sendMessage(CommandSource sender, List<String> messages, Placeholder placeholder) {
        messages.forEach(message -> sendMessage(sender, message, placeholder));
    }

    public static void sendMessage(CommandSource sender, String message) {
        sendMessage(sender, message, null);
    }

    public static void sendMessage(CommandSource sender, String message, Placeholder placeholder) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        // Check if message starts with 'actionbar;'
        if (sender instanceof Player player && message.startsWith("actionbar;")) {
            player.sendActionBar(component(message.substring("actionbar;".length())));
        } else {
            sender.sendMessage(component(message.startsWith("actionbar;")
                    ? message.substring("actionbar;".length()) : message));
        }
    }

    public static List<String> color(List<String> messages) {
        return Colors.color(messages);
    }

    /**
     * Legacy string output. Cannot retain click or hover events. Use
     * {@link #component(String)} when the message needs the full format range.
     */
    public static String color(String message) {
        return Colors.color(message);
    }

    public static List<Component> component(List<String> messages) {
        return Colors.component(messages);
    }

    public static Component component(String message) {
        return Colors.component(message);
    }

    public static Component component(String message, TagResolver resolver) {
        return Colors.component(message, resolver);
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(ticks * 50L);
    }

}
