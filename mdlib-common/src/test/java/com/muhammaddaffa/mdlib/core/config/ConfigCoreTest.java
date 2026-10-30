package com.muhammaddaffa.mdlib.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigCoreTest {

    @TempDir
    Path directory;

    @AfterEach
    void forget() {
        AbstractConfigFile.clearAll();
    }

    private static YamlConfigUpdater.ResourceOpener bundled(String path, String yaml) {
        return requested -> requested.equals(path) ? new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)) : null;
    }

    @Test
    void updaterAddsMissingKeysAndKeepsUserValuesAndComments() throws IOException {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "# mine\nprefix: custom\n");

        YamlConfigUpdater.update(bundled("config.yml", "prefix: default\nsection:\n  # new\n  enabled: true\n"),
                "config.yml", file.toFile(), List.of());

        String result = Files.readString(file);
        assertTrue(result.contains("# mine"));
        assertTrue(result.contains("# new"));
        Map<String, Object> values = new Yaml().load(result);
        assertEquals("custom", values.get("prefix"));
        assertEquals(Map.of("enabled", true), values.get("section"));
    }

    @Test
    void updaterSkipsIgnoredPathsAndMatchesTypedKeys() throws IOException {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "'1': user\nrewards:\n  custom: 1\n");

        YamlConfigUpdater.update(bundled("config.yml", "1: default\nrewards:\n  daily: 5\nnew: x\n"),
                "config.yml", file.toFile(), List.of("rewards"));

        assertEquals(Map.of("1", "user", "rewards", Map.of("custom", 1), "new", "x"),
                new Yaml().load(Files.readString(file)));
    }

    @Test
    void updaterRejectsBlankIgnoredPathsAndMissingResources() {
        File file = directory.resolve("config.yml").toFile();
        assertThrows(IllegalArgumentException.class,
                () -> YamlConfigUpdater.update(bundled("config.yml", "a: 1\n"), "config.yml", file, List.of(" ")));
        IOException missing = assertThrows(IOException.class,
                () -> YamlConfigUpdater.update(bundled("config.yml", "a: 1\n"), "other.yml", file, List.of()));
        assertEquals("Missing bundled config: other.yml", missing.getMessage());
    }

    @Test
    void configFilesCopyTheirResourceOnceAndReloadOnlyWhenAsked() throws IOException {
        TestConfig reloading = new TestConfig("menus\\shop.yml", "", true, bundled("menus/shop.yml", "size: 27\n"));
        TestConfig fixed = new TestConfig("data.yml", "store", false, bundled("store/data.yml", "size: 9\n"));

        assertEquals(directory.resolve("menus/shop.yml").toFile(), reloading.getFile());
        assertEquals("size: 27\n", Files.readString(reloading.getFile().toPath()));
        assertEquals(directory.resolve("store/data.yml").toFile(), fixed.getFile());
        assertEquals(1, reloading.loads);

        AbstractConfigFile.reloadAll();

        assertEquals(2, reloading.loads);
        assertEquals(1, fixed.loads);
        assertThrows(IllegalArgumentException.class,
                () -> new TestConfig("missing.yml", null, true, bundled("other.yml", "")));
    }

    @Test
    void updateAllOnlyTouchesConfigsThatOptIn() throws IOException {
        TestConfig config = new TestConfig("config.yml", null, true, bundled("config.yml", "a: 1\n"));
        Files.writeString(config.getFile().toPath(), "b: 2\n");

        AbstractConfigFile.updateAll();
        assertEquals("b: 2\n", Files.readString(config.getFile().toPath()));

        config.setShouldUpdate(true);
        config.addIgnored("c");
        config.addIgnored("c");
        assertThrows(IllegalArgumentException.class, () -> config.addIgnored(""));
        AbstractConfigFile.updateAll();

        assertEquals(Map.of("a", 1, "b", 2), new Yaml().load(Files.readString(config.getFile().toPath())));
    }

    private final class TestConfig extends AbstractConfigFile {
        private int loads;

        private TestConfig(String name, String folder, boolean reload, YamlConfigUpdater.ResourceOpener resources) {
            super(name, folder, reload, directory.toFile(), resources);
            reloadConfig();
        }

        @Override public String getString(String path, String defaultValue) { return defaultValue; }
        @Override public List<String> getStringList(String path) { return List.of(); }
        @Override public int getInt(String path, int defaultValue) { return defaultValue; }
        @Override public List<Integer> getIntegerList(String path) { return List.of(); }
        @Override public double getDouble(String path, double defaultValue) { return defaultValue; }
        @Override public boolean getBoolean(String path) { return false; }
        @Override public long getLong(String path) { return 0; }
        @Override public boolean isConfigurationSection(String path) { return false; }
        @Override public void saveConfig() { }
        @Override public void reloadConfig() { loads++; }
    }
}
