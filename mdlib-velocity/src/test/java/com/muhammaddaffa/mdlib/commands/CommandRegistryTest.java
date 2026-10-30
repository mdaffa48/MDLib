package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.BaseCommand;
import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.muhammaddaffa.mdlib.commands.internal.VelocityCommandWrapper;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommandRegistryTest {

    private final Object plugin = new Object();
    private final List<CommandMeta.Builder> builders = new ArrayList<>();
    private final List<CommandMeta> metas = new ArrayList<>();
    private CommandManager manager;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        ProxyServer server = mock(ProxyServer.class);
        manager = mock(CommandManager.class);
        when(server.getCommandManager()).thenReturn(manager);
        when(manager.metaBuilder(anyString())).thenAnswer(invocation -> {
            CommandMeta.Builder builder = mock(CommandMeta.Builder.class, RETURNS_SELF);
            CommandMeta meta = mock(CommandMeta.class);
            when(builder.build()).thenReturn(meta);
            builders.add(builder);
            metas.add(meta);
            return builder;
        });
        registry = new CommandRegistry(server, plugin);
    }

    private static TestCommand spec(String name, String... aliases) {
        return new TestCommand(name, "perm." + name, List.of(aliases));
    }

    @Test
    void registerBuildsMetaWithAliasesAndPlugin() {
        registry.register(spec("shop", "store", "st"));

        verify(manager).metaBuilder("shop");
        verify(builders.get(0)).aliases("store", "st");
        verify(builders.get(0)).plugin(plugin);
        verify(manager).register(eq(metas.get(0)), any(VelocityCommandWrapper.class));
    }

    @Test
    void registeringOwnNameOrAliasAgainReplacesOldRegistration() {
        registry.register(spec("shop", "store"));
        registry.register(spec("SHOP"));
        registry.register(spec("market", "Store"));

        verify(manager).unregister(metas.get(0));
        verify(manager, never()).unregister(metas.get(1));
        verify(manager, never()).unregister(anyString());
        verify(manager, times(3)).register(any(CommandMeta.class), any(VelocityCommandWrapper.class));
    }

    @Test
    void unregisterMatchesNamesAndAliasesCaseInsensitively() {
        registry.registerAll(List.of(spec("shop", "store"), spec("bank")));

        registry.unregister("STORE");
        registry.unregister("other", List.of("Bank"));

        verify(manager).unregister(metas.get(0));
        verify(manager).unregister(metas.get(1));
    }

    @Test
    void unregisterLeavesOtherPluginsCommandsAlone() {
        registry.register(spec("shop"));

        registry.unregister("foreign");
        registry.unregister("foreign", List.of("alias"));

        verify(manager, never()).unregister(any(CommandMeta.class));
        verify(manager, never()).unregister(anyString());
    }

    @Test
    void unregisterAllRemovesOnlyOwnCommandsOnce() {
        registry.registerAll(List.of(spec("shop"), spec("bank")));

        registry.unregisterAll();
        registry.unregisterAll();

        verify(manager).unregister(metas.get(0));
        verify(manager).unregister(metas.get(1));
        verify(manager, never()).unregister(anyString());
    }

    @Test
    void wrapperDelegatesToSpecWithInvokedAlias() {
        TestCommand command = spec("shop");
        registry.register(command);
        ArgumentCaptor<VelocityCommandWrapper> wrapper = ArgumentCaptor.forClass(VelocityCommandWrapper.class);
        verify(manager).register(eq(metas.get(0)), wrapper.capture());
        CommandSource source = mock(CommandSource.class);
        SimpleCommand.Invocation invocation = mock(SimpleCommand.Invocation.class);
        when(invocation.source()).thenReturn(source);
        when(invocation.alias()).thenReturn("st");
        when(invocation.arguments()).thenReturn(new String[]{"a", "b"});

        wrapper.getValue().execute(invocation);

        assertSame(source, command.lastSender);
        assertEquals("st", command.lastLabel);
        assertEquals(List.of("a", "b"), Arrays.asList(command.lastArgs));
        assertEquals(List.of("st:a,b"), wrapper.getValue().suggest(invocation));
        assertFalse(wrapper.getValue().hasPermission(invocation));
        when(source.hasPermission("perm.shop")).thenReturn(true);
        assertTrue(wrapper.getValue().hasPermission(invocation));
    }

    @Test
    void wrapperAllowsEveryoneWithoutRootPermission() {
        SimpleCommand.Invocation invocation = mock(SimpleCommand.Invocation.class);
        when(invocation.source()).thenReturn(mock(CommandSource.class));

        assertTrue(new VelocityCommandWrapper(new TestCommand("open", " ", List.of())).hasPermission(invocation));
        assertTrue(new VelocityCommandWrapper(new TestCommand("open", null, List.of())).hasPermission(invocation));
    }

    static final class TestCommand extends BaseCommand implements SimpleCommandSpec {
        private final List<String> aliases;
        CommandSource lastSender;
        String lastLabel;
        String[] lastArgs;

        TestCommand(String name, String permission, List<String> aliases) {
            super(name, permission);
            this.aliases = aliases;
        }

        @Override
        public List<String> aliases() {
            return aliases;
        }

        @Override
        public void execute(CommandSource sender, String label, String[] args) {
            lastSender = sender;
            lastLabel = label;
            lastArgs = args;
        }

        @Override
        public List<String> tabComplete(CommandSource sender, String alias, String[] args) {
            return List.of(alias + ":" + String.join(",", args));
        }
    }

}
