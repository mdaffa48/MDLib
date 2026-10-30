package com.muhammaddaffa.mdlib.configupdater;

import com.muhammaddaffa.mdlib.core.config.YamlConfigUpdater;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class ConfigUpdater {

    private ConfigUpdater() {
    }

    public static void update(Plugin plugin, String resourcePath, File file, String... ignoredPaths) throws IOException {
        update(plugin, resourcePath, file, List.of(ignoredPaths));
    }

    public static void update(Plugin plugin, String resourcePath, File file, List<String> ignoredPaths) throws IOException {
        YamlConfigUpdater.update(plugin::getResource, resourcePath, file, ignoredPaths);
    }
}
