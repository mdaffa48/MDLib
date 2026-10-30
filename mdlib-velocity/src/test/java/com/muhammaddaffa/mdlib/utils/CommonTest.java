package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommonTest {

    @TempDir
    Path directory;
    private ProxyServer server;
    private Player player;
    private ConsoleCommandSource console;

    @BeforeEach
    void setUp() {
        server = mock(ProxyServer.class);
        player = mock(Player.class);
        console = mock(ConsoleCommandSource.class);
        when(server.getCommandManager()).thenReturn(mock(CommandManager.class));
        when(server.getConsoleCommandSource()).thenReturn(console);
        MDLib.onEnable(new Object(), server, mock(org.slf4j.Logger.class), directory);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    @Test
    void actionbarPrefixGoesToPlayerActionBar() {
        Common.sendMessage(player, "actionbar;&aHi %name%", new Placeholder().add("%name%", "Alex"));

        verify(player).sendActionBar(Common.component("&aHi Alex"));
        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void actionbarPrefixIsStrippedForConsole() {
        Common.sendMessage(console, "actionbar;hello");

        verify(console).sendMessage(Common.component("hello"));
    }

    @Test
    void placeholdersTranslateAndEmptyMessagesAreSkipped() {
        CommandSource sender = mock(CommandSource.class);

        Common.sendMessage(sender, "Hello %name%", new Placeholder().add("%name%", "Bob"));
        Common.sendMessage(sender, (String) null);
        Common.sendMessage(sender, "");
        Common.sendMessage(sender, List.of("one", "two"));

        verify(sender).sendMessage(Common.component("Hello Bob"));
        verify(sender).sendMessage(Common.component("one"));
        verify(sender).sendMessage(Common.component("two"));
        verify(sender, times(3)).sendMessage(any(Component.class));
    }

    @Test
    void broadcastReachesEveryPlayerAndConsole() {
        Player other = mock(Player.class);
        when(server.getAllPlayers()).thenReturn(List.of(player, other));

        Common.broadcast("&eNews %what%", new Placeholder().add("%what%", "today"));

        Component expected = Common.component("&eNews today");
        verify(player).sendMessage(expected);
        verify(other).sendMessage(expected);
        verify(console).sendMessage(expected);
    }

    @Test
    void titlesUseTickDurations() {
        Common.sendTitle(player, "&aTitle %n%", "Sub %n%", new Placeholder().add("%n%", "1"));
        Common.sendTitle(player, "T", "S", 10, 20, 30);

        ArgumentCaptor<Title> titles = ArgumentCaptor.forClass(Title.class);
        verify(player, times(2)).showTitle(titles.capture());
        Title first = titles.getAllValues().get(0);
        Title second = titles.getAllValues().get(1);
        assertEquals(Common.component("&aTitle 1"), first.title());
        assertEquals(Common.component("Sub 1"), first.subtitle());
        assertEquals(Title.Times.times(Duration.ofMillis(1000), Duration.ofMillis(2000), Duration.ofMillis(1000)), first.times());
        assertEquals(Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1000), Duration.ofMillis(1500)), second.times());
    }

    @Test
    void actionBarTranslatesPlaceholder() {
        Common.actionBar(player, "Hi %p%", new Placeholder().add("%p%", "Sam"));

        verify(player).sendActionBar(Common.component("Hi Sam"));
    }

    @Test
    void shutdownClearsTagResolvers() {
        Component unresolved = Common.component("<greet>");
        Common.registerTagResolver("greet", TagResolver.resolver("greet", Tag.preProcessParsed("hi")));
        assertNotEquals(unresolved, Common.component("<greet>"));

        MDLib.shutdown();

        assertEquals(unresolved, Common.component("<greet>"));
    }

    @Test
    void formatReadsNotationFromConfig() {
        CommentedConfigurationNode config = CommentedConfigurationNode.root();
        config.node("currency-notation", "thousand").raw("k");

        // Same pattern and default locale as Numbers, so the expected strings hold on any machine.
        DecimalFormat format = new DecimalFormat("###,###,###,###,###.##");
        assertEquals(format.format(1.5) + "k", Common.format(config, 1500));
        assertEquals(format.format(2) + "M", Common.format(config, 2_000_000));
    }

}
