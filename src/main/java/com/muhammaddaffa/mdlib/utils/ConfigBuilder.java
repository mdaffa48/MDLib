package com.muhammaddaffa.mdlib.utils;

public class ConfigBuilder {

    private final String configName;
    private String directory;
    private boolean shouldReload = true;
    private boolean shouldAutoUpdate = false;

    public ConfigBuilder(String configName) {
        this.configName = configName;
    }

    public ConfigBuilder setDirectory(String directory) {
        this.directory = directory;
        return this;
    }

    public ConfigBuilder setShouldReload(boolean shouldReload) {
        this.shouldReload = shouldReload;
        return this;
    }

    public ConfigBuilder setShouldAutoUpdate(boolean shouldAutoUpdate) {
        this.shouldAutoUpdate = shouldAutoUpdate;
        return this;
    }

    public Config build() {
        Config config = new Config(configName, directory, shouldReload);
        config.setShouldUpdate(shouldAutoUpdate);
        return config;
    }

    public static ConfigBuilder create(String configName) {
        return new ConfigBuilder(configName);
    }

}
