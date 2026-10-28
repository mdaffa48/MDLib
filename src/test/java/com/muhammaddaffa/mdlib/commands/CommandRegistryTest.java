package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.muhammaddaffa.mdlib.commands.internal.BukkitCommandWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandRegistryTest {

    private MockedStatic<Bukkit> bukkit;
    private SimpleCommandMap commandMap;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        commandMap = new SimpleCommandMap(mock(Server.class), new HashMap<>());
        bukkit.when(Bukkit::getCommandMap).thenReturn(commandMap);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("TestPlugin");
        registry = new CommandRegistry(plugin);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void unregisterByNameRemovesEveryAliasAndUnregistersTheCommand() {
        registry.register(new TestSpec("example", List.of("one", "two")));
        Command command = commandMap.getCommand("example");
        assertTrue(command.isRegistered());
        assertSame(command, commandMap.getCommand("one"));
        assertSame(command, commandMap.getCommand("testplugin:two"));

        registry.unregister("EXAMPLE");

        assertFalse(commandMap.getKnownCommands().containsValue(command));
        assertFalse(command.isRegistered());
        assertNull(commandMap.getCommand("one"));
        assertNull(commandMap.getCommand("testplugin:two"));
    }

    @Test
    void replacementRemovesAliasesThatAreAbsentFromTheNewSpec() {
        registry.register(new TestSpec("example", List.of("old")));
        Command old = commandMap.getCommand("example");

        registry.register(new TestSpec("example", List.of("new")));

        Command replacement = commandMap.getCommand("example");
        assertNotSame(old, replacement);
        assertFalse(old.isRegistered());
        assertFalse(commandMap.getKnownCommands().containsValue(old));
        assertNull(commandMap.getCommand("old"));
        assertNull(commandMap.getCommand("testplugin:old"));
        assertSame(replacement, commandMap.getCommand("new"));
        assertSame(replacement, commandMap.getCommand("testplugin:new"));
        registry.unregisterAll();
        assertFalse(replacement.isRegistered());
        assertFalse(commandMap.getKnownCommands().containsValue(replacement));
    }

    @Test
    void collisionsOverrideForeignLabelsAndRestoreThemOnShutdown() {
        Plugin otherPlugin = mock(Plugin.class);
        when(otherPlugin.getName()).thenReturn("OtherPlugin");
        Command foreign = new BukkitCommandWrapper(otherPlugin,
                new TestSpec("example", List.of("shared", "unrelated")));
        commandMap.register("otherplugin", foreign);

        registry.register(new TestSpec("example", List.of("shared", "own")));
        Command owned = commandMap.getCommand("testplugin:example");
        assertNotSame(foreign, owned);
        assertSame(owned, commandMap.getCommand("example"));
        assertSame(owned, commandMap.getCommand("shared"));
        assertSame(foreign, commandMap.getCommand("unrelated"));
        assertSame(foreign, commandMap.getCommand("otherplugin:example"));
        assertTrue(foreign.isRegistered());
        assertEquals("example", owned.getLabel());

        registry.register(new TestSpec("example", List.of("replacement")));
        Command replacement = commandMap.getCommand("example");
        assertNotSame(owned, replacement);
        assertFalse(owned.isRegistered());
        assertNull(commandMap.getCommand("own"));
        assertNull(commandMap.getCommand("testplugin:shared"));
        registry.unregisterAll();

        assertFalse(replacement.isRegistered());
        assertSame(foreign, commandMap.getCommand("example"));
        assertSame(foreign, commandMap.getCommand("shared"));
        assertSame(foreign, commandMap.getCommand("otherplugin:example"));
        assertNull(commandMap.getCommand("own"));
    }

    @Test
    void explicitUnregisterCanDisplaceForeignCommandBeforeRegistration() {
        Command foreign = new BukkitCommandWrapper(mock(Plugin.class), new TestSpec("sell", List.of()));
        commandMap.register("otherplugin", foreign);
        registry.unregister("sell");
        assertNull(commandMap.getCommand("sell"));
        registry.register(new TestSpec("sell", List.of()));
        assertNotSame(foreign, commandMap.getCommand("sell"));
        registry.unregisterAll();
        assertSame(foreign, commandMap.getCommand("sell"));
    }

    @Test
    void cleanupDoesNotOverwriteANewerForeignRegistration() {
        Command foreign = new BukkitCommandWrapper(mock(Plugin.class), new TestSpec("example", List.of()));
        commandMap.register("otherplugin", foreign);
        registry.register(new TestSpec("example", List.of()));
        Command newer = new BukkitCommandWrapper(mock(Plugin.class), new TestSpec("example", List.of()));
        commandMap.getKnownCommands().put("example", newer);
        registry.unregisterAll();
        assertSame(newer, commandMap.getCommand("example"));
    }

    @Test
    void commandMapAccessFallsBackWhenPaperApiIsAbsent() {
        Server server = mock(Server.class);
        when(server.getCommandMap()).thenReturn(commandMap);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        bukkit.when(Bukkit::getCommandMap).thenThrow(new NoSuchMethodError("Spigot fixture"));
        CommandRegistry legacy = new CommandRegistry(mock(Plugin.class));
        // Constructor success proves the reflective server method supplied the map.
        legacy.unregisterAll();
    }

    @Test
    void knownCommandsAccessFallsBackToInheritedBackingField() {
        var backing = new HashMap<String, Command>();
        SimpleCommandMap legacyMap = spy(new SimpleCommandMap(mock(Server.class), backing));
        doThrow(new NoSuchMethodError("Spigot fixture")).when(legacyMap).getKnownCommands();
        bukkit.when(Bukkit::getCommandMap).thenReturn(legacyMap);
        Plugin owner = mock(Plugin.class);
        when(owner.getName()).thenReturn("LegacyPlugin");
        CommandRegistry legacy = new CommandRegistry(owner);
        legacy.register(new TestSpec("legacy", List.of("old")));
        assertNotNull(backing.get("legacy"));
        legacy.unregisterAll();
        assertNull(backing.get("legacy"));
        assertNull(backing.get("old"));
    }

    @Test
    void unregisterByAliasRemovesOnlyItsOwnedCommand() {
        registry.register(new TestSpec("example", List.of("one")));
        registry.register(new TestSpec("unrelated", List.of("other")));
        Command removed = commandMap.getCommand("example");
        Command unrelated = commandMap.getCommand("unrelated");

        registry.unregister("one");

        assertFalse(removed.isRegistered());
        assertFalse(commandMap.getKnownCommands().containsValue(removed));
        assertSame(unrelated, commandMap.getCommand("unrelated"));
        assertSame(unrelated, commandMap.getCommand("other"));
        assertTrue(unrelated.isRegistered());
    }

    private record TestSpec(String name, List<String> aliases) implements SimpleCommandSpec {

        @Override
        public void execute(CommandSender sender, String label, String[] args) {
        }
    }
}
