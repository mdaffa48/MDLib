package com.muhammaddaffa.mdlib;

import com.muhammaddaffa.mdlib.gui.SimpleInventory;
import com.muhammaddaffa.mdlib.gui.SimpleInventoryManager;
import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.worldguards.listeners.RegionListener;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MDLib {

    private static JavaPlugin instance;

    public static boolean VERBOSE_OUTPUT = false;
    public static boolean SILENT_LOGS = true;

    private static boolean PLACEHOLDER_API, VAULT, WORLD_GUARD;

    /**
     * This method should be executed in JavaPlugin#onLoad
     */
    public static void inject(JavaPlugin plugin) {
        // load the command api
        CommandAPI.onLoad(new CommandAPIBukkitConfig(plugin)
                .verboseOutput(VERBOSE_OUTPUT)
                .silentLogs(SILENT_LOGS));
    }

    /**
     * This method should be executed in JavaPlugin#onEnable
     */
    public static void onEnable(JavaPlugin plugin) {
        // enable the command api
        CommandAPI.onEnable();
        // initialize the instance
        instance = plugin;
        // check if server is using placeholderapi
        PLACEHOLDER_API = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        VAULT = Bukkit.getPluginManager().getPlugin("VAULT") != null;
        WORLD_GUARD = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        // Register the listeners
        registerListeners();
    }

    /**
     * This method should be executed in JavaPlugin#onDisable
     * @return
     */
    public static void shutdown() {
        CommandAPI.onDisable();
    }

    private static void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        // Register events
        if (usingWorldGuard()) {
            pm.registerEvents(new RegionListener(), instance);
        }

        // If using vault, register the VaultEconomy
        if (usingVault()) {
            VaultEconomy.init();
        }

        // Register the gui library
        SimpleInventoryManager.register(instance);
    }

    public static boolean usingWorldGuard() {
        return WORLD_GUARD;
    }

    public static boolean usingPlaceholderAPI() {
        return PLACEHOLDER_API;
    }

    public static boolean usingVault() {
        return VAULT;
    }

    public static JavaPlugin getInstance() {
        return instance;
    }

}
