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
    void collisionsPreserveForeignCommandsWhileOwnedNamespacedEntriesAreRemoved() {
        Plugin otherPlugin = mock(Plugin.class);
        when(otherPlugin.getName()).thenReturn("OtherPlugin");
        Command foreign = new BukkitCommandWrapper(otherPlugin,
                new TestSpec("example", List.of("shared")));
        commandMap.register("otherplugin", foreign);

        registry.register(new TestSpec("example", List.of("shared", "own")));
        Command owned = commandMap.getCommand("testplugin:example");
        assertNotSame(foreign, owned);
        assertSame(foreign, commandMap.getCommand("example"));
        assertSame(foreign, commandMap.getCommand("shared"));
        assertEquals("testplugin:example", owned.getLabel());

        registry.register(new TestSpec("example", List.of("replacement")));
        Command replacement = commandMap.getCommand("testplugin:example");
        assertNotSame(owned, replacement);
        assertFalse(owned.isRegistered());
        assertNull(commandMap.getCommand("own"));
        assertNull(commandMap.getCommand("testplugin:shared"));
        registry.unregister("example", List.of("shared"));

        assertFalse(replacement.isRegistered());
        assertFalse(commandMap.getKnownCommands().containsValue(replacement));
        assertSame(foreign, commandMap.getCommand("example"));
        assertSame(foreign, commandMap.getCommand("shared"));
        assertSame(foreign, commandMap.getCommand("otherplugin:example"));
        assertTrue(foreign.isRegistered());
        registry.unregister("example", List.of("shared"));
        assertSame(foreign, commandMap.getCommand("example"));
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
