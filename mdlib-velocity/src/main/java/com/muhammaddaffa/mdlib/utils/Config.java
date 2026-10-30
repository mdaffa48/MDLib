package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.core.config.AbstractConfigFile;
import com.velocitypowered.api.command.CommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private CommentedConfigurationNode config;

    public Config(String configName, String directory, boolean shouldReload) {
        super(configName, directory, shouldReload, MDLib.dataDirectory().toFile(),
                MDLib.instance().getClass().getClassLoader()::getResourceAsStream);
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

    public void sendMessage(CommandSource sender, String path) {
        this.sendMessage(sender, path, null);
    }

    public void sendMessage(CommandSource sender, String path, @Nullable Placeholder placeholder) {
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

    public CommentedConfigurationNode getConfig() {
        return config;
    }

    public String getString(String path, String defaultValue) {
        Object value = this.node(path).raw();
        return value != null ? value.toString() : defaultValue;
    }

    public List<String> getStringList(String path) {
        List<String> result = new ArrayList<>();
        CommentedConfigurationNode node = this.node(path);
        if (!node.isList()) {
            return result;
        }
        for (CommentedConfigurationNode child : node.childrenList()) {
            Object value = child.rawScalar();
            if (value != null) {
                result.add(String.valueOf(value));
            }
        }
        return result;
    }

    public int getInt(String path, int defaultValue) {
        return this.node(path).rawScalar() instanceof Number number ? number.intValue() : defaultValue;
    }

    public List<Integer> getIntegerList(String path) {
        List<Integer> result = new ArrayList<>();
        CommentedConfigurationNode node = this.node(path);
        if (!node.isList()) {
            return result;
        }
        for (CommentedConfigurationNode child : node.childrenList()) {
            Object value = child.rawScalar();
            if (value instanceof Number number) {
                result.add(number.intValue());
            } else if (value instanceof String string) {
                try {
                    result.add(Integer.valueOf(string));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    public double getDouble(String path, double defaultValue) {
        return this.node(path).rawScalar() instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public boolean getBoolean(String path) {
        return this.node(path).rawScalar() instanceof Boolean value && value;
    }

    public long getLong(String path) {
        return this.node(path).rawScalar() instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * The section at the dotted path, or null when the path is not a section.
     */
    public CommentedConfigurationNode getConfigurationSection(String path) {
        CommentedConfigurationNode node = this.node(path);
        return node.isMap() ? node : null;
    }

    public boolean isConfigurationSection(String path) {
        return this.node(path).isMap();
    }

    /**
     * Writes the config to its file. Configurate's YAML writer does not keep the file's comments.
     */
    public void saveConfig() {
        try {
            this.loader().save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        try {
            this.config = this.loader().load();
        } catch (IOException ex) {
            Logger.severe("Cannot load " + getFile() + ": " + ex.getMessage());
            this.config = CommentedConfigurationNode.root();
        }
    }

    private YamlConfigurationLoader loader() {
        return YamlConfigurationLoader.builder()
                .path(getFile().toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();
    }

    // Walks a Bukkit-style dotted path, matching keys by their string form so numeric YAML keys like "1" resolve
    private CommentedConfigurationNode node(String path) {
        CommentedConfigurationNode node = config;
        for (String key : path.split("\\.")) {
            CommentedConfigurationNode child = node.node(key);
            if (child.virtual()) {
                for (Map.Entry<Object, CommentedConfigurationNode> entry : node.childrenMap().entrySet()) {
                    if (String.valueOf(entry.getKey()).equals(key)) {
                        child = entry.getValue();
                        break;
                    }
                }
            }
            node = child;
        }
        return node;
    }

}
