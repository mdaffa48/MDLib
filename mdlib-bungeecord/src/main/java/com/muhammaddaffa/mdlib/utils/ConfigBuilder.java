package com.muhammaddaffa.mdlib.utils;

import java.util.ArrayList;
import java.util.List;

public class ConfigBuilder {

    private final String configName;
    private String directory;
    private boolean shouldReload = true;
    private boolean shouldAutoUpdate = false;
    private final List<String> ignored = new ArrayList<>();

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

    public ConfigBuilder addIgnored(String... paths) {
        if (paths == null) {
            throw new IllegalArgumentException("Ignored paths must not be null");
        }
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Ignored path must not be null or blank");
            }
            if (!ignored.contains(path)) {
                ignored.add(path);
            }
        }
        return this;
    }

    public Config build() {
        Config config = new Config(configName, directory, shouldReload);
        config.setShouldUpdate(shouldAutoUpdate);
        ignored.forEach(config::addIgnored);
        return config;
    }

    public static ConfigBuilder create(String configName) {
        return new ConfigBuilder(configName);
    }

}
