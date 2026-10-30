package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandRegistryTest {

    private PluginManager plugins;
    private Plugin plugin;
    private Plugin other;
    private CommandRegistry registry;

    static final class Foreign extends Command {

        Foreign(String name, String... aliases) {
            super(name, null, aliases);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
        }
    }

    @BeforeEach
    void setUp() {
        ProxyServer proxy = mock(ProxyServer.class);
        when(proxy.getLogger()).thenReturn(Logger.getLogger("CommandRegistryTest"));
        plugins = new PluginManager(proxy);
        when(proxy.getPluginManager()).thenReturn(plugins);
        plugin = mock(Plugin.class);
        other = mock(Plugin.class);
        when(plugin.getProxy()).thenReturn(proxy);
        registry = new CommandRegistry(plugin);
    }

    private Map<String, Command> commands() {
        Map<String, Command> commands = new HashMap<>();
        plugins.getCommands().forEach(entry -> commands.put(entry.getKey(), entry.getValue()));
        return commands;
    }

    private static RoutedCommand command(String name, String... aliases) {
        return new RoutedCommand(name) {}.alias(aliases);
    }

    @Test
    void registeringAnOwnedNameOrAliasReplacesTheOldCommand() {
        registry.registerAll(List.of(command("shop", "store"), command("bank")));
        Command shop = commands().get("shop");
        assertSame(shop, commands().get("store"));

        registry.register(command("market", "STORE"));

        Map<String, Command> commands = commands();
        assertEquals(3, commands.size());
        assertFalse(commands.containsKey("shop"));
        assertSame(commands.get("market"), commands.get("store"));
        assertNotSame(shop, commands.get("market"));
        assertTrue(commands.containsKey("bank"));
    }

    @Test
    void unregisterOnlyRemovesThisRegistrysCommands() {
        registry.register(command("foo"));
        registry.register(command("baz"));
        Foreign lookAlike = new Foreign("foo");
        Foreign bar = new Foreign("bar");
        plugins.registerCommand(other, lookAlike);
        plugins.registerCommand(other, bar);

        registry.unregister("FOO");
        registry.unregister("bar");

        Map<String, Command> commands = commands();
        assertSame(lookAlike, commands.get("foo"));
        assertSame(bar, commands.get("bar"));
        assertTrue(commands.containsKey("baz"));
    }

    @Test
    void anotherPluginsLookAlikeDoesNotRemoveOrKeepOurCommand() {
        Foreign lookAlike = new Foreign("foo");
        plugins.registerCommand(other, lookAlike);
        registry.register(command("foo"));
        Command ours = commands().get("foo");

        plugins.unregisterCommands(other);
        assertSame(ours, commands().get("foo"));

        plugins.registerCommand(other, lookAlike);
        registry.unregisterAll();
        assertSame(lookAlike, commands().get("foo"));
    }

    @Test
    void unregisterAllLeavesOtherPluginsCommands() {
        registry.register(command("one", "uno"));
        registry.register(command("two"));
        Foreign host = new Foreign("host", "h");
        plugins.registerCommand(other, host);

        registry.unregisterAll();

        assertEquals(Map.of("host", host, "h", host), commands());
    }

}
