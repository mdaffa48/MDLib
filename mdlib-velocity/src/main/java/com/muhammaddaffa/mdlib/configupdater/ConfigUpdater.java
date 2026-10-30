package com.muhammaddaffa.mdlib.configupdater;

import com.muhammaddaffa.mdlib.core.config.YamlConfigUpdater;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class ConfigUpdater {

    private ConfigUpdater() {
    }

    public static void update(Object plugin, String resourcePath, File file, String... ignoredPaths) throws IOException {
        update(plugin, resourcePath, file, List.of(ignoredPaths));
    }

    /**
     * Adds missing keys from the YAML resource bundled in {@code plugin}'s jar, found through its class loader.
     */
    public static void update(Object plugin, String resourcePath, File file, List<String> ignoredPaths) throws IOException {
        YamlConfigUpdater.update(path -> plugin.getClass().getClassLoader().getResourceAsStream(path), resourcePath, file, ignoredPaths);
    }
}
