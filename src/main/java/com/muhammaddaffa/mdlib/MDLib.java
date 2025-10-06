package com.muhammaddaffa.mdlib;

import com.jeff_media.customblockdata.CustomBlockData;
import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.mdlib.worldguards.listeners.entity.EntityRegionListener;
import com.muhammaddaffa.mdlib.worldguards.listeners.RegionListener;
import com.muhammaddaffa.mdlib.worldguards.listeners.entity.EntityRemoveListenerV1_20_4;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPISpigotConfig;
import fr.mrmicky.fastinv.FastInvManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MDLib {

    private static JavaPlugin instance;

    public static boolean VERBOSE_OUTPUT = false;
    public static boolean SILENT_LOGS = true;
    public static boolean LISTEN_WORLDGUARD = false;
    public static boolean CUSTOM_BLOCK_DATA = false;
    public static boolean COMMANDAPI = true;

    private static boolean PLACEHOLDER_API, VAULT, WORLD_GUARD;

    /**
     * This method should be executed in JavaPlugin#onLoad
     */
    public static void inject(JavaPlugin plugin) {
        // load the command api
        if (COMMANDAPI) {
            CommandAPI.onLoad(new CommandAPISpigotConfig(plugin)
                    .verboseOutput(VERBOSE_OUTPUT)
                    .silentLogs(SILENT_LOGS));
        }
    }

    /**
     * This method should be executed in JavaPlugin#onEnable
     */
    public static void onEnable(JavaPlugin plugin) {
        // enable the command api
        if (COMMANDAPI) {
            CommandAPI.onEnable();
        }
        // initialize the instance
        instance = plugin;
        // check if server is using placeholderapi
        PLACEHOLDER_API = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        VAULT = Bukkit.getPluginManager().getPlugin("Vault") != null;
        WORLD_GUARD = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        // Register the listeners
        registerListeners();
    }

    /**
     * This method should be executed in JavaPlugin#onDisable
     * @return
     */
    public static void shutdown() {
        if (COMMANDAPI) {
            CommandAPI.onDisable();
        }
    }

    private static void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        // Register events
        if (usingWorldGuard() && LISTEN_WORLDGUARD) {
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
        if (usingVault()) {
            VaultEconomy.init();
        }

        // Custom block data
        if (CUSTOM_BLOCK_DATA) {
            CustomBlockData.registerListener(instance);
        }

        // FastInv
        FastInvManager.register(instance);
    }

    private static boolean usingWorldGuard() {
        return WORLD_GUARD;
    }

    private static boolean usingPlaceholderAPI() {
        return PLACEHOLDER_API;
    }

    private static boolean usingVault() {
        return VAULT;
    }

    public static void registerWorldGuard() {
        LISTEN_WORLDGUARD = true;
    }

    public static void registerCustomBlockData() {
        CUSTOM_BLOCK_DATA = true;
    }

    private static boolean isPaper() {
        try {
            // Any other works, just the shortest I could find.
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean is1_20_4OrNewer() {
        try {
            Class.forName("org.bukkit.event.entity.EntityRemoveEvent;");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static JavaPlugin instance() {
        return instance;
    }

}
