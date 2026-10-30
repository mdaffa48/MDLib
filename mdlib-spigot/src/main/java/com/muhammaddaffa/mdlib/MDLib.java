package com.muhammaddaffa.mdlib;

import com.jeff_media.customblockdata.CustomBlockData;
import com.muhammaddaffa.mdlib.commands.CommandRegistry;
import com.muhammaddaffa.mdlib.core.LogSink;
import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.updatechecker.UpdateChecker;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.mdlib.worldguards.WgEntity;
import com.muhammaddaffa.mdlib.worldguards.listeners.entity.EntityRegionListener;
import com.muhammaddaffa.mdlib.worldguards.listeners.RegionListener;
import com.muhammaddaffa.mdlib.worldguards.listeners.entity.EntityRemoveListenerV1_20_4;

import com.muhammaddaffa.mdlib.fastinv.FastInvManager;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MDLib {

    private static JavaPlugin instance;

    public static boolean LISTEN_VAULT = true;
    public static boolean LISTEN_WORLDGUARD = false;
    public static boolean CUSTOM_BLOCK_DATA = false;

    private static boolean PLACEHOLDER_API, VAULT, WORLD_GUARD;

    private static CommandRegistry commands;
    private static final List<Listener> listeners = new ArrayList<>();

    /**
     * This method should be executed in JavaPlugin#onLoad
     */
    public static void inject(JavaPlugin plugin) {

    }

    /**
     * This method should be executed in JavaPlugin#onEnable
     */
    public static void onEnable(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (instance == plugin) {
            return;
        }
        if (instance != null) {
            throw new IllegalStateException("MDLib is already initialized for another plugin.");
        }
        Set<Listener> existing = HandlerList.getRegisteredListeners(plugin).stream()
                .map(RegisteredListener::getListener).collect(Collectors.toSet());
        instance = plugin;
        LogSink.install((level, message) -> plugin.getLogger().log(level, message));
        boolean enabled = false;
        try {
            commands = new CommandRegistry(plugin);
            PLACEHOLDER_API = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
            VAULT = Bukkit.getPluginManager().isPluginEnabled("Vault");
            WORLD_GUARD = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
            registerListeners();
            enabled = true;
        } finally {
            listeners.addAll(HandlerList.getRegisteredListeners(plugin).stream()
                    .map(RegisteredListener::getListener).filter(listener -> !existing.contains(listener))
                    .distinct().toList());
            if (!enabled) {
                shutdown();
            }
        }
    }

    /**
     * This method should be executed in JavaPlugin#onDisable
     * @return
     */
    public static void shutdown() {
        if (instance != null) {
            UpdateChecker.shutdown(instance);
        }
        ExecutorManager.shutdown();
        if (commands != null) {
            commands.unregisterAll();
        }
        listeners.forEach(HandlerList::unregisterAll);
        listeners.clear();
        Config.shutdown();
        Common.clearTagResolvers();
        if (WORLD_GUARD) {
            WgEntity.getPlayerCache().clear();
        }
        if (VAULT) {
            VaultEconomy.shutdown();
        }
        commands = null;
        instance = null;
        PLACEHOLDER_API = false;
        VAULT = false;
        WORLD_GUARD = false;
        LISTEN_VAULT = true;
        LISTEN_WORLDGUARD = false;
        CUSTOM_BLOCK_DATA = false;
        LogSink.uninstall();
    }

    private static void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        // Register events
        if (WORLD_GUARD && LISTEN_WORLDGUARD) {
            pm.registerEvents(new RegionListener(), instance);
            Logger.info("WorldGuard hook from MDLib is enabled, registering it...");
            // Check if it's version 1.20.4 or newer
            if (is1_20_4OrNewer()) {
                pm.registerEvents(new EntityRemoveListenerV1_20_4(), instance);
                Logger.info("Version is v1.20.4 or newer, registering EntityRemoveEvent listener!");
            }
            // Check if it's using paper
            if (isPaper()) {
                pm.registerEvents(new EntityRegionListener(), instance);
                Logger.info("Using paper, an enhanced MDLib WorldGuard hook has been installed!");
            }
        }

        // If using vault, register the VaultEconomy
        if (VAULT && LISTEN_VAULT) {
            VaultEconomy.init();
        }

        // Custom block data
        if (CUSTOM_BLOCK_DATA) {
            CustomBlockData.registerListener(instance);
        }

        // FastInv
        FastInvManager.register(instance);
    }

    public static CommandRegistry getCommandRegistry() {
        return commands;
    }

    public static void registerWorldGuard() {
        LISTEN_WORLDGUARD = true;
    }

    public static void registerCustomBlockData() {
        CUSTOM_BLOCK_DATA = true;
    }

    public static boolean isPaper() {
        try {
            // Any other works, just the shortest I could find.
            Class.forName("com.destroystokyo.paper.ParticleBuilder", false, MDLib.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean is1_20_4OrNewer() {
        try {
            Class.forName("org.bukkit.event.entity.EntityRemoveEvent", false, MDLib.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Check if the server is using Folia
     * @return true if the server is using Folia, false otherwise
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false, MDLib.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
        String serverName = org.bukkit.Bukkit.getServer().getName();
        if (serverName != null && serverName.equalsIgnoreCase("Folia")) {
            return true;
        }

        String version = org.bukkit.Bukkit.getVersion();
        return version != null && version.toLowerCase(Locale.ROOT).contains("folia");
    }

    public static JavaPlugin instance() {
        return instance;
    }

}
