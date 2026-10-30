package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.configupdater.ConfigUpdater;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class ConfigTest {

    static final class TestPlugin {
    }

    @TempDir
    Path directory;
    private ProxyServer server;
    private org.slf4j.Logger logger;

    @BeforeEach
    void setUp() {
        server = mock(ProxyServer.class);
        logger = mock(org.slf4j.Logger.class);
        when(server.getCommandManager()).thenReturn(mock(CommandManager.class));
        MDLib.onEnable(new TestPlugin(), server, logger, directory);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    private Path file() {
        return directory.resolve("velocity-test.yml");
    }

    @Test
    void bundledResourceIsCopiedOnlyOnce() throws Exception {
        new Config("velocity-test.yml", null, true);
        assertTrue(Files.readString(file()).contains("single: \"&aHello %player%\""));

        Files.writeString(file(), "messages:\n  single: edited\n");
        Config again = new Config("velocity-test.yml", "", true);

        assertEquals("edited", again.getString("messages.single"));
    }

    @Test
    void gettersFollowBukkitSemantics() {
        Config config = new Config("velocity-test.yml", null, true);

        assertEquals("&aHello %player%", config.getString("messages.single"));
        assertEquals("fallback", config.getString("missing.path", "fallback"));
        assertNull(config.getString("missing.path"));
        assertEquals("first", config.getString("levels.1"));
        assertEquals(List.of("line one %player%", "line two"), config.getStringList("messages.multi"));
        assertEquals(List.of(), config.getStringList("messages.single"));
        assertEquals(List.of(), config.getStringList("missing"));
        assertEquals(5, config.getInt("section.amount"));
        assertEquals(7, config.getInt("missing", 7));
        assertEquals(3, config.getInt("messages.single", 3));
        assertEquals(List.of(1, 2), config.getIntegerList("section.numbers"));
        assertEquals(2.5, config.getDouble("section.ratio"));
        assertEquals(5.0, config.getDouble("section.amount"));
        assertEquals(1.5, config.getDouble("missing", 1.5));
        assertTrue(config.getBoolean("section.enabled"));
        assertFalse(config.getBoolean("section.disabled"));
        assertFalse(config.getBoolean("missing"));
        assertFalse(config.getBoolean("section.amount"));
        assertEquals(9_000_000_000L, config.getLong("section.big"));
        assertEquals(0L, config.getLong("missing"));
    }

    @Test
    void configurationSectionIsNullForNonSections() {
        Config config = new Config("velocity-test.yml", null, true);

        assertNotNull(config.getConfigurationSection("section"));
        assertEquals(5, config.getConfigurationSection("section").node("amount").getInt());
        assertTrue(config.isConfigurationSection("messages"));
        assertNull(config.getConfigurationSection("messages.single"));
        assertNull(config.getConfigurationSection("messages.multi"));
        assertNull(config.getConfigurationSection("missing"));
        assertFalse(config.isConfigurationSection("section.amount"));
    }

    @Test
    void sendMessageUsesListBeforeString() {
        Config config = new Config("velocity-test.yml", null, true);
        CommandSource sender = mock(CommandSource.class);
        Placeholder placeholder = new Placeholder().add("%player%", "Steve");

        config.sendMessage(sender, "messages.multi", placeholder);
        config.sendMessage(sender, "messages.single", placeholder);
        config.sendMessage(sender, "missing");

        InOrder order = inOrder(sender);
        order.verify(sender).sendMessage(Common.component("line one Steve"));
        order.verify(sender).sendMessage(Common.component("line two"));
        order.verify(sender).sendMessage(Common.component("&aHello Steve"));
        verify(sender, times(3)).sendMessage(any(Component.class));
    }

    @Test
    void broadcastUsesListBeforeString() {
        ConsoleCommandSource console = mock(ConsoleCommandSource.class);
        when(server.getConsoleCommandSource()).thenReturn(console);
        when(server.getAllPlayers()).thenReturn(List.of());
        Config config = new Config("velocity-test.yml", null, true);

        config.broadcast("messages.multi");

        verify(console).sendMessage(Common.component("line one %player%"));
        verify(console).sendMessage(Common.component("line two"));
    }

    @Test
    void reloadOnlyReloadsConfigsThatAskForIt() throws Exception {
        Config reloading = new Config("velocity-test.yml", null, true);
        Files.createDirectories(directory.resolve("fixed"));
        Files.writeString(directory.resolve("fixed/velocity-test.yml"), "value: 1\n");
        Config fixed = new Config("velocity-test.yml", "fixed", false);

        Files.writeString(file(), "messages:\n  single: reloaded\n");
        Files.writeString(directory.resolve("fixed/velocity-test.yml"), "value: 2\n");
        Config.reload();

        assertEquals("reloaded", reloading.getString("messages.single"));
        assertEquals(1, fixed.getInt("value"));
    }

    @Test
    void saveConfigWritesNodes() throws Exception {
        Config config = new Config("velocity-test.yml", null, true);

        config.getConfig().node("added", "key").raw("value");
        config.saveConfig();
        config.reloadConfig();

        assertEquals("value", config.getString("added.key"));
        assertEquals(5, config.getInt("section.amount"));
        assertTrue(Files.readString(file()).replace("\r\n", "\n").contains("added:\n  key: value"));
    }

    @Test
    void invalidYamlLogsAndLoadsEmptySection() throws Exception {
        Config config = new Config("velocity-test.yml", null, true);
        Files.writeString(file(), "broken: [unclosed\n");

        config.reloadConfig();

        verify(logger).error(startsWith("Cannot load " + file().toFile()));
        assertNotNull(config.getConfig());
        assertNull(config.getString("broken"));
        assertNull(config.getString("messages.single"));
        assertTrue(config.getConfig().empty());
    }

    @Test
    void shutdownForgetsConfigs() throws Exception {
        Config config = new Config("velocity-test.yml", null, true);
        Files.writeString(file(), "messages:\n  single: reloaded\n");

        MDLib.shutdown();
        Config.reload();

        assertEquals("&aHello %player%", config.getString("messages.single"));
    }

    @Test
    void builderAndUpdaterAddMissingKeys() throws Exception {
        Files.writeString(file(), "messages:\n  single: mine\nlevels:\n  2: second\n");
        Config config = ConfigBuilder.create("velocity-test.yml").setShouldAutoUpdate(true).addIgnored("levels").build();

        Config.updateConfigs();
        config.reloadConfig();

        assertTrue(config.isShouldUpdate());
        assertEquals("mine", config.getString("messages.single"));
        assertEquals(5, config.getInt("section.amount"));
        assertEquals("second", config.getString("levels.2"));
        assertNull(config.getString("levels.1"));

        File other = directory.resolve("other.yml").toFile();
        Files.writeString(other.toPath(), "section:\n  amount: 9\n");
        ConfigUpdater.update(MDLib.instance(), "velocity-test.yml", other, "section");
        String updated = Files.readString(other.toPath());
        assertTrue(updated.contains("single: \"&aHello %player%\""));
        assertTrue(updated.contains("amount: 9"));
        assertFalse(updated.contains("ratio:"));
    }

}
