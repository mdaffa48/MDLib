package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.core.config.AbstractConfigFile;
import com.muhammaddaffa.mdlib.core.text.Numbers;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Config extends AbstractConfigFile {

    public static void shutdown() {
        clearAll();
    }

    public static void reload() {
        reloadAll();
    }

    public static void updateConfigs() {
        updateAll();
    }

    // -----------------------------------------------------------

    private Configuration config;

    public Config(String configName, String directory, boolean shouldReload) {
        super(configName, directory, shouldReload, MDLib.instance().getDataFolder(), MDLib.instance()::getResourceAsStream);
        reloadConfig();
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

    public Configuration getConfig() {
        return config;
    }

    public String getString(String path, String defaultValue) {
        return getString(config, path, defaultValue);
    }

    public List<String> getStringList(String path) {
        List<String> result = new ArrayList<>();
        if (get(config, path) instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
                    result.add(String.valueOf(value));
                }
            }
        }
        return result;
    }

    public int getInt(String path, int defaultValue) {
        return get(config, path) instanceof Number number ? number.intValue() : defaultValue;
    }

    public List<Integer> getIntegerList(String path) {
        List<Integer> result = new ArrayList<>();
        if (get(config, path) instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof Number number) {
                    result.add(number.intValue());
                } else if (value instanceof String string && Numbers.isInt(string)) {
                    result.add(Integer.parseInt(string));
                }
            }
        }
        return result;
    }

    public double getDouble(String path, double defaultValue) {
        return get(config, path) instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public boolean getBoolean(String path) {
        return get(config, path) instanceof Boolean value && value;
    }

    public long getLong(String path) {
        return get(config, path) instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * The section at the path, or null when the path is missing or holds a value, like Bukkit.
     */
    public Configuration getConfigurationSection(String path) {
        return get(config, path) instanceof Configuration section ? section : null;
    }

    public boolean isConfigurationSection(String path) {
        return get(config, path) instanceof Configuration;
    }

    /**
     * Writes the config to its file. BungeeCord's YamlConfiguration cannot keep comments, so the file loses them.
     * Like Bukkit, the config is serialized before the file is opened, so a value that cannot be serialized
     * throws and leaves the file unchanged.
     */
    public void saveConfig() {
        try {
            StringWriter data = new StringWriter();
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, data);
            Files.writeString(getFile().toPath(), data.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the file again. A file that cannot be read or parsed is logged and loads as an empty config, like Bukkit.
     */
    public void reloadConfig() {
        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile());
        } catch (Exception ex) {
            Logger.severe("Cannot load " + getFile() + ": " + ex);
            this.config = new Configuration();
        }
    }

    // Reads a dotted path without Configuration#get, which creates missing parent sections and throws when a parent is a value.
    static Object get(Configuration section, String path) {
        Object value = section;
        for (String key : path.split("\\.")) {
            if (!(value instanceof Configuration current)) {
                return null;
            }
            value = current.get(key, null);
        }
        return value;
    }

    static String getString(Configuration section, String path, String defaultValue) {
        Object value = get(section, path);
        return value != null ? value.toString() : defaultValue;
    }

}
