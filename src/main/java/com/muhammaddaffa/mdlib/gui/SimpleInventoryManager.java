package com.muhammaddaffa.mdlib.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for SimpleInventory listeners.
 *
 * @author MrMicky
 */
public final class SimpleInventoryManager {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private SimpleInventoryManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Register listeners for SimpleInventory.
     *
     * @param plugin plugin to register
     * @throws NullPointerException if plugin is null
     * @throws IllegalStateException if SimpleInventory is already registered
     */
    public static void register(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (REGISTERED.getAndSet(true)) {
            throw new IllegalStateException("SimpleInventory is already registered");
        }

        Bukkit.getPluginManager().registerEvents(new InventoryListener(plugin), plugin);
    }

    /**
     * Close all open SimpleInventory inventories.
     */
    public static void closeAll() {
        // This is an alternative solution
        Bukkit.getOnlinePlayers().forEach(Player::closeInventory);

        // Method below throws error on 1.21
        /*try {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getOpenInventory().getTopInventory().getHolder() instanceof SimpleInventory)
                    .forEach(Player::closeInventory);
        } catch (Exception ignored) {}*/
    }

    public static final class InventoryListener implements Listener {

        private final Plugin plugin;

        public InventoryListener(Plugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if (e.getInventory().getHolder() instanceof SimpleInventory && e.getClickedInventory() != null) {
                SimpleInventory inv = (SimpleInventory) e.getInventory().getHolder();

                boolean wasCancelled = e.isCancelled();
                e.setCancelled(true);

                inv.handleClick(e);

                // This prevents un-canceling the event if another plugin canceled it before
                if (!wasCancelled && !e.isCancelled()) {
                    e.setCancelled(false);
                }
            }
        }

        @EventHandler
        public void onInventoryOpen(InventoryOpenEvent e) {
            if (e.getInventory().getHolder() instanceof SimpleInventory) {
                SimpleInventory inv = (SimpleInventory) e.getInventory().getHolder();

                inv.handleOpen(e);
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent e) {
            if (e.getInventory().getHolder() instanceof SimpleInventory) {
                SimpleInventory inv = (SimpleInventory) e.getInventory().getHolder();

                if (inv.handleClose(e)) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> inv.open((Player) e.getPlayer()));
                }
            }
        }

        @EventHandler
        public void onPluginDisable(PluginDisableEvent e) {
            if (e.getPlugin() == this.plugin) {
                closeAll();

                REGISTERED.set(false);
            }
        }
    }
}
