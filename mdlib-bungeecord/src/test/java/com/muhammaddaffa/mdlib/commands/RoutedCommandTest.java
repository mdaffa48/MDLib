package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.BaseCommand;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.commands.internal.BungeeCommandWrapper;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoutedCommandTest {

    private final CommandSender sender = mock(CommandSender.class);
    private final RoutedCommand.Handler handler = mock(RoutedCommand.Handler.class);

    static final class TestCommand extends RoutedCommand {

        TestCommand(String permission) {
            super("help", permission);
        }

        CommandPlan add(String literal) {
            return sub(literal);
        }

        CommandPlan base() {
            return root();
        }
    }

    private List<BaseComponent> sent() {
        ArgumentCaptor<BaseComponent> captor = ArgumentCaptor.forClass(BaseComponent.class);
        verify(sender, atLeast(0)).sendMessage(captor.capture());
        return captor.getAllValues();
    }

    private List<String> plain() {
        return sent().stream().map(component -> component.toPlainText()).toList();
    }

    @Test
    void missingPermissionSendsTheRedPermissionMessage() throws Exception {
        TestCommand command = new TestCommand("help.use");
        command.base().exec(handler);

        command.execute(sender, "help", new String[0]);

        assertEquals(List.of("You don't have permission to use this command."), plain());
        assertTrue(sent().get(0).toLegacyText().contains("§c"));
        verify(handler, never()).run(any(), any());
    }

    @Test
    void badArgumentsSendTheUsageOfTheMatchedSubcommand() throws Exception {
        TestCommand command = new TestCommand(null);
        command.add("set").alias("s").arg("amount", new IntArg()).exec(handler);

        command.execute(sender, "help", new String[]{"S", "x"});
        command.execute(sender, "help", new String[]{"set"});

        assertEquals(List.of("Usage: /help set <number>", "Usage: /help set <number>"), plain());
        verify(handler, never()).run(any(), any());
    }

    @Test
    void unknownSubcommandListsThePermittedSubcommands() {
        TestCommand command = new TestCommand(null);
        command.add("set").perm("help.set").arg("amount", new IntArg()).exec(handler);
        command.add("status").exec(handler);
        command.add("admin").perm("help.admin").exec(handler);
        when(sender.hasPermission("help.set")).thenReturn(true);

        command.execute(sender, "help", new String[]{"nope"});

        assertEquals(List.of("Unknown subcommand: nope", "/help set <number>", "/help status"), plain());
    }

    @Test
    void wrapperRunsTheSpecWithItsPrimaryNameAndCompletesTabs() throws Exception {
        TestCommand command = new TestCommand(" ");
        command.alias("h", "assist");
        command.add("status").exec(handler);
        command.add("stop").exec(handler);
        BungeeCommandWrapper wrapper = new BungeeCommandWrapper(command);

        assertEquals("help", wrapper.getName());
        assertNull(wrapper.getPermission());
        assertArrayEquals(new String[]{"h", "assist"}, wrapper.getAliases());
        assertEquals(List.of("status", "stop"), wrapper.onTabComplete(sender, new String[]{"st"}));

        wrapper.execute(sender, new String[]{"status"});
        verify(handler).run(eq(sender), any());

        command.execute(sender, "help", new String[]{"stop", "extra"});
        assertEquals(List.of("Usage: /help stop"), plain());
    }

    @Test
    void baseCommandKeepsItsPermissionOnTheWrapper() {
        BaseCommand command = new BaseCommand("ping", "", null, "ping.use") {
            @Override
            public void execute(CommandSender sender, String label, String[] args) {
                sender.sendMessage(label);
            }
        };

        BungeeCommandWrapper wrapper = new BungeeCommandWrapper(command);
        wrapper.execute(sender, new String[0]);

        assertEquals("ping.use", wrapper.getPermission());
        assertEquals("/ping", command.usage());
        verify(sender).sendMessage("ping");
    }

    @Test
    void onlinePlayerArgumentLooksPlayersUpOnTheProxy() {
        ProxyServer proxy = mock(ProxyServer.class);
        ProxiedPlayer steve = mock(ProxiedPlayer.class);
        ProxiedPlayer alex = mock(ProxiedPlayer.class);
        when(steve.getName()).thenReturn("Steve");
        when(alex.getName()).thenReturn("Alex");
        when(proxy.getPlayer("steve")).thenReturn(steve);
        when(proxy.getPlayers()).thenReturn(List.of(steve, alex));
        TestCommand command = new TestCommand(null);
        command.add("kick").arg("target", new OnlinePlayerArg()).exec((s, ctx) -> s.sendMessage(ctx.get("target", ProxiedPlayer.class).getName()));

        try (MockedStatic<ProxyServer> statics = mockStatic(ProxyServer.class)) {
            statics.when(ProxyServer::getInstance).thenReturn(proxy);

            assertEquals(List.of("Steve"), command.tabComplete(sender, "help", new String[]{"kick", "ST"}));
            command.execute(sender, "help", new String[]{"kick", "steve"});
            command.execute(sender, "help", new String[]{"kick", "nobody"});
        }

        verify(sender).sendMessage("Steve");
        assertEquals(List.of("Usage: /help kick <player>"), plain());
    }

}
