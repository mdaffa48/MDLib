package com.muhammaddaffa.mdlib;

import com.muhammaddaffa.mdlib.gui.SimpleInventoryManager;
import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MDLib {

    private static JavaPlugin instance;

    private static boolean PLACEHOLDER_API;
    private static boolean VAULT;

    /**
     * This method should be executed in JavaPlugin#onLoad
     */
    public static void inject(JavaPlugin plugin) {
        instance = plugin;
        // load the command api
        CommandAPI.onLoad(new CommandAPIBukkitConfig(plugin));
    }

    /**
     * This method should be executed in JavaPlugin#onEnable
     */
    public static void onEnable() {
        CommandAPI.onEnable();
        // check if server is using placeholderapi
        PLACEHOLDER_API = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        VAULT = Bukkit.getPluginManager().getPlugin("VAULT") != null;
        // register vault
        if (usingVault()) {
            VaultEconomy.init();
        }
        // register the gui library
        SimpleInventoryManager.register(instance);
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
