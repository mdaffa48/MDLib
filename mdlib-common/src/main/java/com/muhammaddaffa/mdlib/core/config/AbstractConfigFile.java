package com.muhammaddaffa.mdlib.core.config;

import com.muhammaddaffa.mdlib.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConfigFile implements ConfigFile {

    private static final Map<String, AbstractConfigFile> configMap = new ConcurrentHashMap<>();

    /**
     * Forgets every config created so far, so {@link #reloadAll()} and {@link #updateAll()} skip them.
     */
    public static void clearAll() {
        configMap.clear();
    }

    /**
     * Reloads every config created with shouldReload set to true.
     */
    public static void reloadAll() {
        for (AbstractConfigFile config : configMap.values()) {
            if (!config.isShouldReload()) continue;
            config.reloadConfig();
        }
    }

    /**
     * Adds missing keys from the bundled resource to the file of every config with shouldUpdate set to true.
     * Like Spigot's Config.updateConfigs, it does not reload them.
     */
    public static void updateAll() {
        for (AbstractConfigFile config : configMap.values()) {
            if (!config.isShouldUpdate()) {
                continue;
            }
            try {
                YamlConfigUpdater.update(config.resources, config.resourcePath, config.getFile(), config.ignored);
            } catch (Exception ex) {
                Logger.severe("Failed to update the " + config.getFile().getName());
                ex.printStackTrace();
            }
        }
    }

    private final File file;
    private final String configName;
    private final String resourcePath;
    private final YamlConfigUpdater.ResourceOpener resources;
    private final boolean shouldReload;

    private boolean shouldUpdate;
    private final List<String> ignored = new ArrayList<>();

    /**
     * Resolves {@code directory/configName} inside {@code dataFolder} and copies the bundled resource there when the
     * file does not exist yet. Subclasses load the file afterwards with {@link #reloadConfig()}.
     */
    protected AbstractConfigFile(String configName, String directory, boolean shouldReload, File dataFolder,
                                 YamlConfigUpdater.ResourceOpener resources) {
        this.configName = configName;
        this.resourcePath = (directory == null || directory.isEmpty() ? configName : directory + "/" + configName)
                .replace('\\', '/').replaceAll("/+", "/");
        this.resources = resources;
        this.shouldReload = shouldReload;

        this.file = new File(dataFolder, resourcePath);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        if (!this.file.exists()) {
            try (InputStream input = resources.open(resourcePath)) {
                if (input == null) {
                    throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
                }
                Files.copy(input, file.toPath());
            } catch (IOException ex) {
                throw new IllegalStateException("Could not save " + resourcePath + " to " + file, ex);
            }
        }

        configMap.put(file.getPath(), this);
    }

    @Override
    public boolean isShouldUpdate() {
        return shouldUpdate;
    }

    @Override
    public void setShouldUpdate(boolean shouldUpdate) {
        this.shouldUpdate = shouldUpdate;
    }

    @Override
    public void addIgnored(String ignoredPath) {
        if (ignoredPath == null || ignoredPath.isBlank()) {
            throw new IllegalArgumentException("Ignored path must not be null or blank");
        }
        if (!this.ignored.contains(ignoredPath)) {
            this.ignored.add(ignoredPath);
        }
    }

    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public boolean isShouldReload() {
        return shouldReload;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getString(String path) {
        return this.getString(path, null);
    }

    @Override
    public int getInt(String path) {
        return this.getInt(path, 0);
    }

    @Override
    public double getDouble(String path) {
        return this.getDouble(path, 0.0);
    }

}
