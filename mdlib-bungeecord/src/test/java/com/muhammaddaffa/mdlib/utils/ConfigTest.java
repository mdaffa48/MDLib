package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.configupdater.ConfigUpdater;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfigTest {

    private static final String YAML = """
            single: hello
            count: 5
            ratio: 2.5
            enabled: true
            disabled: false
            big: 10000000000
            nested:
              deep:
                value: found
            lines:
            - one
            - 2
            numbers: [1, "2", x]
            currency-notation:
              thousand: k
            """;

    public static class Unrepresentable {
        public String getBoom() {
            throw new IllegalStateException("boom");
        }

        public void setBoom(String boom) {
        }
    }

    @TempDir
    Path directory;
    private java.util.logging.Logger logger;

    @BeforeEach
    void setUp() {
        ProxyServer proxy = mock(ProxyServer.class);
        when(proxy.getLogger()).thenReturn(java.util.logging.Logger.getLogger("ConfigTest"));
        PluginManager plugins = new PluginManager(proxy);
        when(proxy.getPluginManager()).thenReturn(plugins);
        logger = mock(java.util.logging.Logger.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getProxy()).thenReturn(proxy);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getResourceAsStream(anyString())).thenAnswer(invocation -> invocation.<String>getArgument(0).endsWith(".yml")
                ? new ByteArrayInputStream(YAML.getBytes(StandardCharsets.UTF_8)) : null);
        MDLib.onEnable(plugin);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    @Test
    void bundledResourceIsCopiedOnceAndGettersReadDottedPaths() throws Exception {
        Config config = new Config("config.yml", "sub", true);
        Path file = directory.resolve("sub/config.yml");

        assertEquals(YAML, Files.readString(file));
        assertEquals("found", config.getString("nested.deep.value"));
        assertEquals("fallback", config.getString("nested.missing", "fallback"));
        assertNull(config.getString("missing"));
        assertEquals("5", config.getString("count"));
        assertEquals(5, config.getInt("count"));
        assertEquals(7, config.getInt("count.inner", 7));
        assertEquals(0, config.getInt("single"));
        assertEquals(2.5, config.getDouble("ratio"));
        assertEquals(1.5, config.getDouble("missing.ratio", 1.5));
        assertTrue(config.getBoolean("enabled"));
        assertFalse(config.getBoolean("missing"));
        assertEquals(10000000000L, config.getLong("big"));
        assertEquals(List.of("one", "2"), config.getStringList("lines"));
        assertEquals(List.of(), config.getStringList("missing.lines"));
        assertEquals(List.of(1, 2), config.getIntegerList("numbers"));
        assertTrue(config.isConfigurationSection("nested.deep"));
        assertNotNull(config.getConfigurationSection("nested"));
        assertNull(config.getConfigurationSection("count"));
        assertNull(config.getConfigurationSection("missing.section"));
        assertFalse(config.isConfigurationSection("single"));
        assertEquals(Common.digits(1.5) + "k", Common.format(config.getConfig(), 1500));
        assertEquals("2M", Common.format(config.getConfig(), 2_000_000));

        config.saveConfig();
        assertFalse(Files.readString(file).contains("missing"));
        assertTrue(Files.readString(file).contains("value: found"));

        Files.writeString(file, "count: 9\n");
        Config again = new Config("config.yml", "sub", true);
        assertEquals("count: 9\n", Files.readString(file));
        assertEquals(9, again.getInt("count"));
    }

    @Test
    void sendMessageUsesTheListOrTheSingleString() {
        Config config = new Config("config.yml", null, true);
        CommandSender sender = mock(CommandSender.class);

        config.sendMessage(sender, "lines");
        config.sendMessage(sender, "single", new Placeholder().add("hello", "bye"));
        config.sendMessage(sender, "missing");

        ArgumentCaptor<BaseComponent> sent = ArgumentCaptor.forClass(BaseComponent.class);
        verify(sender, times(3)).sendMessage(sent.capture());
        assertEquals(List.of("one", "2", "bye"), sent.getAllValues().stream().map(component -> component.toPlainText()).toList());
    }

    @Test
    void reloadOnlyReloadsConfigsThatAskForIt() throws Exception {
        Config reloadable = new Config("config.yml", null, true);
        Config fixed = new Config("fixed.yml", null, false);
        Files.writeString(directory.resolve("config.yml"), "single: changed\n");
        Files.writeString(directory.resolve("fixed.yml"), "single: changed\n");

        Config.reload();

        assertEquals("changed", reloadable.getString("single"));
        assertEquals("hello", fixed.getString("single"));
    }

    @Test
    void brokenFileLoadsAsAnEmptyConfigAndIsLogged() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "single: [unclosed\n");

        Config config = new Config("config.yml", null, true);

        assertNull(config.getString("single"));
        assertTrue(config.getStringList("lines").isEmpty());
        verify(logger).log(eq(Level.SEVERE), contains("Cannot load"));
    }

    @Test
    void parseErrorOnReloadResetsTheConfigToAnEmptySection() throws Exception {
        Config config = new Config("config.yml", null, true);
        assertEquals("hello", config.getString("single"));
        Files.writeString(directory.resolve("config.yml"), "single: [unclosed\n");

        config.reloadConfig();

        assertNull(config.getString("single"));
        assertNotNull(config.getConfig());
        assertTrue(config.getConfig().getKeys().isEmpty());
        verify(logger).log(eq(Level.SEVERE), contains("Cannot load"));
    }

    @Test
    void getBooleanIsFalseForAFalseValueAndAMissingKey() {
        Config config = new Config("config.yml", null, true);

        assertTrue(config.getBoolean("enabled"));
        assertFalse(config.getBoolean("disabled"));
        assertFalse(config.getBoolean("missing"));
    }

    @Test
    void savedValuesAreWrittenToTheConfigFileAndReadBack() throws Exception {
        Path file = directory.resolve("data.yml");
        Files.writeString(file, "balances:\n  steve: 500\n");
        Config config = new Config("data.yml", null, true);
        config.getConfig().set("balances.alex", 25);

        config.saveConfig();
        config.reloadConfig();

        assertEquals(25, config.getInt("balances.alex"));
        assertEquals(500, config.getInt("balances.steve"));
        assertTrue(Files.readString(file).contains("alex: 25"));
    }

    @Test
    void failedSerializationKeepsThePreviousFileContent() throws Exception {
        Path file = directory.resolve("data.yml");
        Files.writeString(file, "balances:\n  steve: 500\n");
        Config config = new Config("data.yml", null, true);
        config.getConfig().set("broken", new Unrepresentable());

        assertThrows(RuntimeException.class, config::saveConfig);

        assertEquals("balances:\n  steve: 500\n", Files.readString(file));
        assertEquals(500, new Config("data.yml", null, true).getInt("balances.steve"));
    }

    @Test
    void builderAndUpdaterAddMissingKeysButLeaveIgnoredPathsAlone() throws Exception {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "single: mine\nnested:\n  other: x\n");
        Config config = ConfigBuilder.create("config.yml").setShouldAutoUpdate(true).addIgnored("nested").build();

        assertTrue(config.isShouldUpdate());
        Config.updateConfigs();

        String updated = Files.readString(file);
        assertTrue(updated.startsWith("single: mine"));
        assertTrue(updated.contains("count: 5"));
        assertTrue(updated.contains("other: x"));
        assertFalse(updated.contains("deep"), updated);

        Path other = directory.resolve("other.yml");
        Files.writeString(other, "count: 1\nnested:\n  other: x\n");
        ConfigUpdater.update(MDLib.instance(), "other.yml", other.toFile(), "nested");
        String otherUpdated = Files.readString(other);
        assertTrue(otherUpdated.contains("ratio: 2.5"));
        assertFalse(otherUpdated.contains("deep"), otherUpdated);
    }

}
