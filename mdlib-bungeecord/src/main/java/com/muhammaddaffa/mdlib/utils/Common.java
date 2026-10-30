package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.core.text.Colors;
import com.muhammaddaffa.mdlib.core.text.Numbers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.Nullable;

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
     * Sends the message to every online player and the console. MDLib sends it as legacy text through
     * TextComponent.fromLegacy, so MiniMessage click and hover events are dropped.
     */
    public static void broadcast(String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        BaseComponent component = legacy(message);
        ProxyServer proxy = ProxyServer.getInstance();
        for (ProxiedPlayer player : proxy.getPlayers()) {
            player.sendMessage(component);
        }
        proxy.getConsole().sendMessage(component);
    }

    public static void actionBar(ProxiedPlayer player, String message) {
        actionBar(player, message, null);
    }

    public static void actionBar(ProxiedPlayer player, String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        // send the action bar message
        player.sendMessage(ChatMessageType.ACTION_BAR, legacy(message));
    }

    public static void sendTitle(ProxiedPlayer player, String title, String subTitle) {
        sendTitle(player, title, subTitle, null);
    }

    public static void sendTitle(ProxiedPlayer player, String title, String subTitle, @Nullable Placeholder placeholder) {
        sendTitle(player, title, subTitle, 20, 40, 20, placeholder);
    }

    public static void sendTitle(ProxiedPlayer player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        sendTitle(player, title, subTitle, fadeIn, stay, fadeOut, null);
    }

    /**
     * Times are in ticks.
     */
    public static void sendTitle(ProxiedPlayer player, String title, String subTitle, int fadeIn, int stay, int fadeOut, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            title = placeholder.translate(title);
            subTitle = placeholder.translate(subTitle);
        }
        ProxyServer.getInstance().createTitle()
                .title(legacy(title))
                .subTitle(legacy(subTitle))
                .fadeIn(fadeIn)
                .stay(stay)
                .fadeOut(fadeOut)
                .send(player);
    }

    public static String digits(Object o) {
        return Numbers.digits(o);
    }

    public static String format(Configuration config, double number) {
        return Numbers.format(number, (path, defaultSuffix) -> Config.getString(config, path, defaultSuffix));
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

    public static void sendMessage(CommandSender sender, List<String> messages) {
        sendMessage(sender, messages, null);
    }

    public static void sendMessage(CommandSender sender, List<String> messages, Placeholder placeholder) {
        messages.forEach(message -> sendMessage(sender, message, placeholder));
    }

    public static void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, message, null);
    }

    /**
     * A message starting with "actionbar;" goes to a player's action bar. MDLib sends it as legacy text through
     * TextComponent.fromLegacy, so MiniMessage click and hover events are dropped.
     */
    public static void sendMessage(CommandSender sender, String message, Placeholder placeholder) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        // Check if message starts with 'actionbar;'
        if (sender instanceof ProxiedPlayer player && message.startsWith("actionbar;")) {
            player.sendMessage(ChatMessageType.ACTION_BAR, legacy(message.substring("actionbar;".length())));
        } else {
            sender.sendMessage(legacy(message.startsWith("actionbar;")
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

    private static BaseComponent legacy(String message) {
        return TextComponent.fromLegacy(message == null ? "" : Colors.color(message));
    }

}
