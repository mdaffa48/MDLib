package com.muhammaddaffa.mdlib.configupdater;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigUpdaterTest {

    @TempDir
    Path directory;

    @Test
    void addsDefaultsWithoutRemovingCustomSectionsOrChangingExistingValues() throws IOException {
        Path file = write("""
                custom:
                  nested:
                    enabled: true
                settings:
                  limit: 8
                  extension:
                    value: custom
                """);
        Plugin plugin = plugin("""
                settings:
                  limit: 25
                  added: true
                new-section:
                  message: hello
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile(), List.of());

        assertEquals(Map.of(
                "custom", Map.of("nested", Map.of("enabled", true)),
                "settings", Map.of("limit", 8, "extension", Map.of("value", "custom"), "added", true),
                "new-section", Map.of("message", "hello")), read(file));
    }

    @Test
    void leavesIgnoredSectionsUntouchedWhileUpdatingTheirSiblings() throws IOException {
        Path file = write("""
                # Owner's rewards
                rewards:
                  # Keep this explanation
                  custom: emerald # Owner's inline note
                  empty: {}
                empty: {}
                settings:
                  enabled: true
                """);
        Plugin plugin = plugin("""
                rewards:
                  custom: diamond
                  default-reward: gold
                  empty:
                    generated: true
                empty:
                  generated: true
                settings:
                  enabled: false
                  # New setting explanation
                  interval: 20
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile(), "rewards", "empty");

        Map<String, Object> result = read(file);
        assertEquals(Map.of("custom", "emerald", "empty", Map.of()), result.get("rewards"));
        assertEquals(Map.of(), result.get("empty"));
        assertEquals(Map.of("enabled", true, "interval", 20), result.get("settings"));
        String updated = Files.readString(file);
        assertTrue(updated.contains("# Owner's rewards"));
        assertTrue(updated.contains("# Keep this explanation"));
        assertTrue(updated.contains("# Owner's inline note"));
        assertTrue(updated.contains("# New setting explanation"));
        assertFalse(updated.contains("default-reward"));
    }

    @Test
    void generatesMissingIgnoredSectionsOnceAndPreservesThemOnLaterUpdates() throws IOException {
        Path file = write("settings: {}\n");
        Plugin plugin = plugin("""
                settings:
                  enabled: true
                rewards:
                  first:
                    item: diamond
                    lore:
                      - First reward
                  second: 2
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile(), "rewards");

        Map<String, Object> expectedRewards = Map.of(
                "first", Map.of("item", "diamond", "lore", List.of("First reward")), "second", 2);
        assertEquals(expectedRewards, read(file).get("rewards"));

        resource(plugin, """
                settings:
                  enabled: false
                  new-setting: true
                rewards:
                  first:
                    item: gold
                    new-child: true
                  third: 3
                """);
        ConfigUpdater.update(plugin, "config.yml", file.toFile(), "rewards");

        assertEquals(expectedRewards, read(file).get("rewards"));
        assertEquals(Map.of("enabled", true, "new-setting", true), read(file).get("settings"));
    }

    @Test
    void preservesListsExplicitNullsAndConflictingValueTypes() throws IOException {
        Path file = write("""
                list:
                  - custom
                  - other
                nullable: null
                scalar: custom
                section:
                  custom: true
                """);
        Plugin plugin = plugin("""
                list:
                  - default
                nullable:
                  generated: true
                scalar:
                  generated: true
                section: default
                added: true
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        Map<String, Object> result = read(file);
        assertEquals(List.of("custom", "other"), result.get("list"));
        assertTrue(result.containsKey("nullable"));
        assertNull(result.get("nullable"));
        assertEquals("custom", result.get("scalar"));
        assertEquals(Map.of("custom", true), result.get("section"));
        assertEquals(true, result.get("added"));
    }

    @Test
    void matchesEquivalentTypedKeysWithoutReplacingCustomValues() throws Exception {
        Path file = write("01: custom-number\non: custom-boolean\n");
        Plugin plugin = plugin("1: default-number\ntrue: default-boolean\nadded: true\n");

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        YamlConfiguration result = new YamlConfiguration();
        result.load(file.toFile());
        assertEquals(Set.of("1", "true", "added"), result.getKeys(false));
        assertEquals("custom-number", result.getString("1"));
        assertEquals("custom-boolean", result.getString("true"));
        assertTrue(result.getBoolean("added"));
    }

    @Test
    void preservesDottedOwnerValuesWhenNestedDefaultsAddSiblings() throws Exception {
        Path file = write("# Owner's limit\nsettings.limit: 8 # Keep this value\n");
        Plugin plugin = plugin("""
                settings:
                  limit: 25
                  # New setting explanation
                  added: true
                  group:
                    enabled: true
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        YamlConfiguration result = new YamlConfiguration();
        result.load(file.toFile());
        assertEquals(8, result.getInt("settings.limit"));
        assertTrue(result.getBoolean("settings.added"));
        assertTrue(result.getBoolean("settings.group.enabled"));
        String updated = Files.readString(file);
        assertTrue(updated.contains("settings.limit: 8"));
        assertTrue(updated.contains("# Owner's limit"));
        assertTrue(updated.contains("# Keep this value"));
        assertTrue(updated.contains("# New setting explanation"));

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        assertEquals(updated, Files.readString(file));
    }

    @Test
    void preservesNestedOwnerValuesWhenDottedDefaultsAddSiblings() throws Exception {
        Path file = write("""
                settings:
                  limit: 8
                  group:
                    custom: owner
                """);
        Plugin plugin = plugin("""
                settings.limit: 25
                settings.added: true
                settings.group:
                  custom: default
                  enabled: true
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        YamlConfiguration result = new YamlConfiguration();
        result.load(file.toFile());
        assertEquals(8, result.getInt("settings.limit"));
        assertEquals("owner", result.getString("settings.group.custom"));
        assertTrue(result.getBoolean("settings.group.enabled"));
        assertTrue(result.getBoolean("settings.added"));
    }

    @Test
    void honorsIgnoredSectionsAcrossDottedAndNestedPaths() throws Exception {
        for (String original : List.of("settings.limit: 8\n", "settings:\n  limit: 8\n")) {
            for (String defaults : List.of(
                    "settings:\n  limit: 25\n  added: true\nsettings-extra:\n  added: true\n",
                    "settings.limit: 25\nsettings.added: true\nsettings-extra.added: true\n")) {
                Path file = write(original);

                ConfigUpdater.update(plugin(defaults), "config.yml", file.toFile(), "settings");

                YamlConfiguration result = new YamlConfiguration();
                result.load(file.toFile());
                assertEquals(8, result.getInt("settings.limit"));
                assertFalse(result.contains("settings.added"));
                assertTrue(result.getBoolean("settings-extra.added"));
            }
        }
    }

    @Test
    void preservesBlockingOwnerValuesWhenDefaultsUseDottedDescendants() throws Exception {
        Path file = write("settings: custom\nnullable: null\n");
        Plugin plugin = plugin("settings.limit: 25\nnullable.generated: true\nadded: true\n");

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        YamlConfiguration result = new YamlConfiguration();
        result.load(file.toFile());
        assertEquals("custom", result.getString("settings"));
        assertFalse(result.contains("settings.limit"));
        assertFalse(result.contains("nullable.generated"));
        assertTrue(result.getBoolean("added"));
        assertTrue(read(file).containsKey("nullable"));
        assertNull(read(file).get("nullable"));
    }

    @Test
    void addsMissingValuesToTheEffectiveSectionWhenOwnerPathsOverlap() throws Exception {
        Path file = write("""
                settings:
                  limit: 8
                settings.group:
                  custom: owner
                """);
        Plugin plugin = plugin("""
                settings:
                  limit: 25
                  group:
                    custom: default
                    added: true
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        YamlConfiguration result = new YamlConfiguration();
        result.load(file.toFile());
        assertEquals(8, result.getInt("settings.limit"));
        assertEquals("owner", result.getString("settings.group.custom"));
        assertTrue(result.getBoolean("settings.group.added"));
    }

    @Test
    void matchesIgnoredPathsBySegmentsAndHandlesOverlappingPaths() throws IOException {
        Path file = write("""
                menu:
                  items:
                    custom: true
                menu-settings: {}
                screens:
                  main: {}
                  main-extra: {}
                """);
        Plugin plugin = plugin("""
                menu:
                  items:
                    default: true
                  title: Main
                menu-settings:
                  enabled: true
                screens:
                  main:
                    title: Main
                  main-extra:
                    title: Extra
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile(),
                List.of("menu", "menu.items", "menu", "screens.main"));

        assertEquals(Map.of(
                "menu", Map.of("items", Map.of("custom", true)),
                "menu-settings", Map.of("enabled", true),
                "screens", Map.of("main", Map.of(), "main-extra", Map.of("title", "Extra"))), read(file));
    }

    @Test
    void doesNotRewriteAnUnchangedFile() throws IOException {
        String original = "# Keep the original spacing\r\nsettings: {value: 'custom'}\r\ncustom:    true\r\n";
        Path file = write(original);
        Files.setLastModifiedTime(file, FileTime.fromMillis(946684800000L));
        FileTime timestamp = Files.getLastModifiedTime(file);
        Plugin plugin = plugin("settings:\n  value: default\n");

        ConfigUpdater.update(plugin, "config.yml", file.toFile(), List.of());

        assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
        assertEquals(timestamp, Files.getLastModifiedTime(file));
    }

    @Test
    void initializesAnEmptyConfigFromDefaults() throws IOException {
        Path file = write("");
        Plugin plugin = plugin("""
                # Config header
                rewards:
                  default: diamond
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile(), "rewards");

        assertEquals(Map.of("rewards", Map.of("default", "diamond")), read(file));
        assertTrue(Files.readString(file).contains("# Config header"));
    }

    @Test
    void initializesACommentOnlyConfigWithoutDiscardingItsHeader() throws IOException {
        Path file = write("# Owner's config header\n# Keep these instructions\n\n");
        Plugin plugin = plugin("settings:\n  enabled: true\n");

        ConfigUpdater.update(plugin, "config.yml", file.toFile());

        assertEquals(Map.of("settings", Map.of("enabled", true)), read(file));
        String updated = Files.readString(file);
        assertTrue(updated.contains("# Owner's config header"));
        assertTrue(updated.contains("# Keep these instructions"));
    }

    @Test
    void preservesYamlMergeValuesAndUpdatesLocalSectionsWithoutChangingIgnoredSources() throws IOException {
        Path file = write("""
                template: &base
                  enabled: true
                  cooldown: 10
                  nested:
                    inherited: original
                local:
                  <<: *base
                  nested:
                    custom: owner
                inherited:
                  <<: *base
                """);
        Plugin plugin = plugin("""
                template:
                  added: false
                local:
                  enabled: false
                  cooldown: 50
                  nested:
                    custom: default
                    added: true
                inherited:
                  enabled: false
                  cooldown: 50
                  nested:
                    inherited: default
                    added: true
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile(), "template");

        assertEquals(Map.of(
                "template", Map.of("enabled", true, "cooldown", 10,
                        "nested", Map.of("inherited", "original")),
                "local", Map.of("enabled", true, "cooldown", 10,
                        "nested", Map.of("custom", "owner", "added", true)),
                "inherited", Map.of("enabled", true, "cooldown", 10,
                        "nested", Map.of("inherited", "original", "added", true))), read(file));
    }

    @Test
    void isolatesIgnoredSectionsFromChangesThroughYamlAliases() throws IOException {
        Path file = write("""
                kept-template: &kept
                  custom: first
                active-alias: *kept
                active-template: &active
                  custom: second
                kept-alias: *active
                """);
        Plugin plugin = plugin("""
                kept-template:
                  added: true
                active-alias:
                  added: true
                active-template:
                  added: true
                kept-alias:
                  added: true
                """);

        ConfigUpdater.update(plugin, "config.yml", file.toFile(), "kept-template", "kept-alias");

        assertEquals(Map.of(
                "kept-template", Map.of("custom", "first"),
                "active-alias", Map.of("custom", "first", "added", true),
                "active-template", Map.of("custom", "second", "added", true),
                "kept-alias", Map.of("custom", "second")), read(file));
    }

    @Test
    void leavesTheOriginalFileIntactWhenTheResourceIsMissing() throws IOException {
        Path file = write("custom: preserved\n");
        Plugin plugin = mock(Plugin.class);
        byte[] original = Files.readAllBytes(file);

        assertThrows(IOException.class,
                () -> ConfigUpdater.update(plugin, "config.yml", file.toFile()));

        assertArrayEquals(original, Files.readAllBytes(file));
        assertOnlyConfigFileRemains();
    }

    @Test
    void refusesInvalidDestinationYamlWithoutRewritingIt() throws IOException {
        Plugin plugin = plugin("added: true\n");
        for (String invalid : invalidDocuments()) {
            Path file = write(invalid);
            byte[] original = Files.readAllBytes(file);

            assertThrows(IOException.class,
                    () -> ConfigUpdater.update(plugin, "config.yml", file.toFile()), invalid);

            assertArrayEquals(original, Files.readAllBytes(file), invalid);
            assertOnlyConfigFileRemains();
        }
    }

    @Test
    void refusesInvalidDefaultsWithoutRewritingTheDestination() throws IOException {
        Path file = write("custom: preserved\n");
        byte[] original = Files.readAllBytes(file);
        for (String invalid : invalidDocuments()) {
            Plugin plugin = plugin(invalid);

            assertThrows(IOException.class,
                    () -> ConfigUpdater.update(plugin, "config.yml", file.toFile()), invalid);

            assertArrayEquals(original, Files.readAllBytes(file), invalid);
            assertOnlyConfigFileRemains();
        }
    }

    private List<String> invalidDocuments() {
        return List.of(
                "broken: [\n",
                "duplicate: first\nduplicate: second\n",
                "section:\n  duplicate: first\n  duplicate: second\n",
                "01: a\n1: b\n",
                "on: a\ntrue: b\n",
                "cycle: &cycle\n  child: *cycle\n",
                "- list\n- root\n",
                "scalar-root\n");
    }

    private Path write(String yaml) throws IOException {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return file;
    }

    private Plugin plugin(String defaults) {
        Plugin plugin = mock(Plugin.class);
        resource(plugin, defaults);
        return plugin;
    }

    private void resource(Plugin plugin, String defaults) {
        when(plugin.getResource("config.yml")).thenAnswer(invocation ->
                new ByteArrayInputStream(defaults.getBytes(StandardCharsets.UTF_8)));
    }

    private Map<String, Object> read(Path file) throws IOException {
        return new Yaml().load(Files.readString(file, StandardCharsets.UTF_8));
    }

    private void assertOnlyConfigFileRemains() throws IOException {
        try (var files = Files.list(directory)) {
            assertEquals(Set.of("config.yml"), files.map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet()));
        }
    }
}
