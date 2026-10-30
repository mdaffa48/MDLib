package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfigBuilderTest {

    @TempDir
    Path directory;

    private final Map<String, String> resources = new HashMap<>();
    private JavaPlugin plugin;
    private MockedStatic<MDLib> mdlib;

    @BeforeEach
    void setUp() {
        Config.shutdown();
        plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getResource(anyString())).thenAnswer(invocation -> {
            String text = resources.get(invocation.<String>getArgument(0));
            return text == null ? null : new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        });
        doAnswer(invocation -> {
            String resourcePath = invocation.getArgument(0);
            writeConfig(resourcePath, resources.get(resourcePath));
            return null;
        }).when(plugin).saveResource(anyString(), eq(false));
        mdlib = mockStatic(MDLib.class);
        mdlib.when(MDLib::instance).thenReturn(plugin);
    }

    @AfterEach
    void tearDown() {
        mdlib.close();
        Config.shutdown();
    }

    @Test
    void repeatedBuilderCallsAccumulateIgnoredPathsAndGenerateMissingSections() throws IOException {
        writeConfig("config.yml", """
                menu:
                  existing: custom
                rewards:
                  existing: custom
                """);
        resources.put("config.yml", """
                menu:
                  existing: default
                  added: true
                rewards:
                  existing: default
                  added: true
                locale:
                  language: en
                  messages:
                    welcome: Hello
                enabled: true
                """);

        ConfigBuilder builder = ConfigBuilder.create("config.yml").setShouldAutoUpdate(true);
        assertSame(builder, builder.addIgnored("menu", "rewards"));
        assertSame(builder, builder.addIgnored("menu", "locale"));
        Config config = builder.build();

        Config.updateConfigs();

        YamlConfiguration updated = readConfig(config);
        assertEquals("custom", updated.getString("menu.existing"));
        assertFalse(updated.contains("menu.added"));
        assertEquals("custom", updated.getString("rewards.existing"));
        assertFalse(updated.contains("rewards.added"));
        assertEquals("en", updated.getString("locale.language"));
        assertEquals("Hello", updated.getString("locale.messages.welcome"));
        assertTrue(updated.getBoolean("enabled"));
    }

    @Test
    void ignoredPathsAreIndependentForEachConfig() throws IOException {
        String original = "menu:\n  existing: custom\n";
        String defaults = "menu:\n  existing: default\n  added: true\n";
        writeConfig("first.yml", original);
        writeConfig("second.yml", original);
        resources.put("first.yml", defaults);
        resources.put("second.yml", defaults);
        Config first = ConfigBuilder.create("first.yml")
                .setShouldAutoUpdate(true)
                .addIgnored("menu")
                .build();
        Config second = ConfigBuilder.create("second.yml")
                .setShouldAutoUpdate(true)
                .build();

        Config.updateConfigs();

        assertFalse(readConfig(first).contains("menu.added"));
        assertTrue(readConfig(second).getBoolean("menu.added"));
    }

    @Test
    void reusingBuilderCopiesIgnoredPathsInsteadOfSharingThem() throws IOException {
        String original = """
                menu:
                  existing: custom
                rewards:
                  existing: custom
                locale:
                  existing: custom
                """;
        String defaults = """
                menu:
                  added: true
                rewards:
                  added: true
                locale:
                  added: true
                """;
        writeConfig("first/config.yml", original);
        writeConfig("second/config.yml", original);
        resources.put("first/config.yml", defaults);
        resources.put("second/config.yml", defaults);
        ConfigBuilder builder = ConfigBuilder.create("config.yml")
                .setShouldAutoUpdate(true)
                .addIgnored("menu");
        Config first = builder.setDirectory("first").build();
        first.addIgnored("locale");
        Config second = builder.setDirectory("second").addIgnored("rewards").build();

        Config.updateConfigs();

        YamlConfiguration firstUpdated = readConfig(first);
        YamlConfiguration secondUpdated = readConfig(second);
        assertFalse(firstUpdated.contains("menu.added"));
        assertTrue(firstUpdated.getBoolean("rewards.added"));
        assertFalse(firstUpdated.contains("locale.added"));
        assertFalse(secondUpdated.contains("menu.added"));
        assertFalse(secondUpdated.contains("rewards.added"));
        assertTrue(secondUpdated.getBoolean("locale.added"));
    }

    @Test
    void existingConfigAddIgnoredMethodProtectsOnlyTheMatchingSection() throws IOException {
        writeConfig("config.yml", """
                menu:
                  items:
                    custom: present
                menu-settings:
                  enabled: false
                """);
        resources.put("config.yml", """
                menu:
                  items:
                    generated: true
                  title: Menu
                menu-settings:
                  added: true
                """);
        Config config = ConfigBuilder.create("config.yml").setShouldAutoUpdate(true).build();
        config.addIgnored("menu.items");
        config.addIgnored("menu.items");

        Config.updateConfigs();

        YamlConfiguration updated = readConfig(config);
        assertEquals("present", updated.getString("menu.items.custom"));
        assertFalse(updated.contains("menu.items.generated"));
        assertEquals("Menu", updated.getString("menu.title"));
        assertTrue(updated.getBoolean("menu-settings.added"));
    }

    @Test
    void saveAndUpdateUseTheNormalizedDirectoryResourceWithoutImplicitReload() {
        resources.put("menus/main/config.yml", "value: original\n");
        Config config = ConfigBuilder.create("config.yml")
                .setDirectory("menus\\main\\")
                .setShouldAutoUpdate(true)
                .build();
        verify(plugin).saveResource("menus/main/config.yml", false);
        resources.put("menus/main/config.yml", "value: default\nadded: true\n");

        Config.updateConfigs();

        verify(plugin).getResource("menus/main/config.yml");
        assertEquals(directory.resolve("menus/main/config.yml"), config.getFile().toPath());
        assertTrue(readConfig(config).getBoolean("added"));
        assertFalse(config.getConfig().contains("added"));
        config.reloadConfig();
        assertTrue(config.getBoolean("added"));
        assertEquals("original", config.getString("value"));
    }

    @Test
    void configsOnlyUpdateWhenEnabled() throws IOException {
        writeConfig("config.yml", "value: original\n");
        resources.put("config.yml", "value: default\nadded: true\n");
        Config config = ConfigBuilder.create("config.yml").addIgnored("value").build();

        Config.updateConfigs();

        verify(plugin, never()).getResource(anyString());
        assertFalse(readConfig(config).contains("added"));

        config.setShouldUpdate(true);
        Config.updateConfigs();
        assertTrue(readConfig(config).getBoolean("added"));
    }

    @Test
    void builderCopiesTheVarargsArray() throws IOException {
        writeConfig("config.yml", "menu:\n  custom: true\n");
        resources.put("config.yml", "menu:\n  added: true\n");
        String[] ignored = {"menu"};
        ConfigBuilder builder = ConfigBuilder.create("config.yml")
                .setShouldAutoUpdate(true)
                .addIgnored(ignored);
        ignored[0] = "unrelated";
        Config config = builder.build();

        Config.updateConfigs();

        assertFalse(readConfig(config).contains("menu.added"));
    }

    @Test
    void invalidIgnoredPathsAreRejected() throws IOException {
        ConfigBuilder builder = ConfigBuilder.create("config.yml");
        assertThrows(IllegalArgumentException.class, () -> builder.addIgnored((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> builder.addIgnored((String) null));
        assertThrows(IllegalArgumentException.class, () -> builder.addIgnored(""));
        assertThrows(IllegalArgumentException.class, () -> builder.addIgnored(" \t"));
        assertSame(builder, builder.addIgnored());
        writeConfig("config.yml", "value: original\n");
        Config config = builder.build();
        assertThrows(IllegalArgumentException.class, () -> config.addIgnored(null));
        assertThrows(IllegalArgumentException.class, () -> config.addIgnored(""));
        assertThrows(IllegalArgumentException.class, () -> config.addIgnored(" \t"));
    }

    private void writeConfig(String resourcePath, String content) throws IOException {
        Path file = directory.resolve(resourcePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private YamlConfiguration readConfig(Config config) {
        return YamlConfiguration.loadConfiguration(config.getFile());
    }
}
