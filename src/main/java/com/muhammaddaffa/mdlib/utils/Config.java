package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    private static final Map<String, Config> configMap = new HashMap<>();

    private static void registerConfig(Config config) {
        configMap.put(config.getFile().getPath(), config);
    }

    public static void reload() {
        for (Config config : configMap.values()) {
            if (!config.isShouldReload()) continue;
            config.reloadConfig();
        }
    }

    // -----------------------------------------------------------

    private final File file;
    private FileConfiguration config;

    private final String configName;
    private boolean shouldReload = false;

    public Config(String configName, String directory, boolean shouldReload) {
        JavaPlugin plugin = MDLib.instance();
        this.configName = configName;
        this.shouldReload = shouldReload;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (directory == null) {
            this.file = new File(plugin.getDataFolder(), configName);

            if (!this.file.exists()) {
                plugin.saveResource(configName, false);
            }

        } else {
            File directoryFile = new File(plugin.getDataFolder() + File.separator + directory);
            if (!directoryFile.exists()) {
                directoryFile.mkdirs();
            }

            this.file = new File(plugin.getDataFolder() + File.separator + directory, configName);

            if (!this.file.exists()) {
                plugin.saveResource(directory + File.separator + configName, false);
            }

        }

        this.config = YamlConfiguration.loadConfiguration(file);
        // Register the config
        Config.registerConfig(this);
    }

    public void broadcast(String path) {
        this.broadcast(path, null);
    }

    public void broadcast(String path, @Nullable Placeholder placeholder) {
        List<String> messages = this.getStringList(path);
        // Check if the messages are empty
        if (messages.isEmpty()) {
            // Send one line message
            Common.broadcast(this.getString(path), placeholder);
        } else {
            // Send multiple line messages
            messages.forEach(message ->
                    Common.broadcast(message, placeholder));
        }
    }

    public void sendMessage(CommandSender sender, String path) {
        this.sendMessage(sender, path, null);
    }

    public void sendMessage(CommandSender sender, String path, @Nullable Placeholder placeholder) {
        List<String> messages = this.getStringList(path);
        // Check if the messages are empty
        if (messages.isEmpty()) {
            // Send one line message
            Common.sendMessage(sender, this.getString(path), placeholder);
        } else {
            // Send multiple line messages
            messages.forEach(message ->
                    Common.sendMessage(sender, message, placeholder));
        }
    }

    public String getConfigName() {
        return configName;
    }

    public boolean isShouldReload() {
        return shouldReload;
    }

    public File getFile() {
        return file;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getString(String path) {
        return this.getString(path, null);
    }

    public String getString(String path, String defaultValue) {
        return this.getConfig().getString(path, defaultValue);
    }

    public List<String> getStringList(String path) {
        return this.getConfig().getStringList(path);
    }

    public int getInt(String path) {
        return this.getInt(path, 0);
    }

    public int getInt(String path, int defaultValue) {
        return this.getConfig().getInt(path, defaultValue);
    }

    public List<Integer> getIntegerList(String path) {
        return this.getConfig().getIntegerList(path);
    }

    public double getDouble(String path) {
        return this.getDouble(path, 0.0);
    }

    public double getDouble(String path, double defaultValue) {
        return this.getConfig().getDouble(path, defaultValue);
    }

    public boolean getBoolean(String path) {
        return this.getConfig().getBoolean(path);
    }

    public long getLong(String path) {
        return this.getConfig().getLong(path);
    }

    public Location getLocation(String path) {
        return this.getConfig().getLocation(path);
    }

    public ItemStack getItemStack(String path) {
        return this.getConfig().getItemStack(path);
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return this.getConfig().getConfigurationSection(path);
    }

    public boolean isConfigurationSection(String path) {
        return this.getConfig().isConfigurationSection(path);
    }

    public void saveConfig() {
        try {
            this.config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

}
