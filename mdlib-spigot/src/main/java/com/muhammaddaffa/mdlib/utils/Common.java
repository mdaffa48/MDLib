package com.muhammaddaffa.mdlib.utils;

import com.cryptomorin.xseries.XSound;
import com.muhammaddaffa.mdlib.core.text.Colors;
import com.muhammaddaffa.mdlib.core.text.Numbers;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        Component original = component(message);
        Set<CommandSender> recipients = new HashSet<>();
        for (Permissible permissible : Bukkit.getPluginManager().getPermissionSubscriptions(Server.BROADCAST_CHANNEL_USERS)) {
            if (permissible instanceof CommandSender sender && sender.hasPermission(Server.BROADCAST_CHANNEL_USERS)) {
                recipients.add(sender);
            }
        }
        BroadcastMessageEvent event = new BroadcastMessageEvent(!Bukkit.isPrimaryThread(), original, recipients);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        for (CommandSender recipient : event.getRecipients()) {
            Component rendered = event.message();
            if (recipient instanceof Player player) {
                rendered = rendered.equals(original) ? component(papi(player, message))
                        : rendered.replaceText(builder -> builder.match("%[^%]+%")
                                .replacement((match, text) -> component(papi(player, match.group()))));
            }
            recipient.sendMessage(rendered);
        }
    }

    public static void actionBar(Player player, String message) {
        actionBar(player, message, null);
    }

    public static void actionBar(Player player, String message, @Nullable Placeholder placeholder) {
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        // send the action bar message
        player.sendActionBar(component(papi(player, message)));
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
        player.showTitle(Title.title(component(papi(player, title)), component(papi(player, subTitle)),
                Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut))));
    }

    public static String digits(Object o) {
        return Numbers.digits(o);
    }

    public static String format(FileConfiguration config, double number) {
        return Numbers.format(number, config::getString);
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
        if (placeholder != null) {
            message = placeholder.translate(message);
        }
        message = papi(sender, message);
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

    public static String papi(CommandSender sender, String message) {
        return sender instanceof Player player ? papi(player, message) : message;
    }

    public static String papi(Player player, String message) {
        if (message == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return message;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, message);
        } catch (Exception ex) {
            return message;
        }
    }

}
