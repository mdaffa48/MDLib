package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommonDeliveryTest {

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<PlaceholderAPI> papi;
    private PluginManager plugins;
    private Player alice;
    private Player bob;
    private ConsoleCommandSender console;

    @BeforeEach
    void setUp() {
        plugins = mock(PluginManager.class);
        alice = mock(Player.class);
        bob = mock(Player.class);
        console = mock(ConsoleCommandSender.class);
        when(alice.getName()).thenReturn("Alice");
        when(bob.getName()).thenReturn("Bob");
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        when(plugins.isPluginEnabled("PlaceholderAPI")).thenReturn(true);
        papi = mockStatic(PlaceholderAPI.class);
        papi.when(() -> PlaceholderAPI.setPlaceholders(any(Player.class), anyString()))
                .thenAnswer(invocation -> invocation.<String>getArgument(1)
                        .replace("%player_name%", invocation.<Player>getArgument(0).getName()));
    }

    @AfterEach
    void tearDown() {
        if (papi != null) {
            papi.close();
        }
        if (bukkit != null) {
            bukkit.close();
        }
    }

    @Test
    void configStringsAndListsApplyMdlibThenPapiBeforeMiniMessage() {
        Config config = mock(Config.class, CALLS_REAL_METHODS);
        YamlConfiguration yaml = new YamlConfiguration();
        doReturn(yaml).when(config).getConfig();
        yaml.set("single", "<green>Hello {name}");
        yaml.set("list", List.of("<click:run_command:'/hello %player_name%'>Hi</click>",
                "actionbar;<gold>{name}"));
        Placeholder placeholder = new Placeholder().add("{name}", "%player_name%");

        config.sendMessage(alice, "single", placeholder);
        config.sendMessage(alice, "list", placeholder);

        verify(alice).sendMessage(Common.component("<green>Hello Alice"));
        verify(alice).sendMessage(Common.component("<click:run_command:'/hello Alice'>Hi</click>"));
        verify(alice).sendActionBar(Common.component("<gold>Alice"));
    }

    @Test
    void directMessagesActionBarsAndTitlesUseTheTargetPlayer() {
        Common.sendMessage(alice, List.of("Hello %player_name%", "actionbar;%player_name%"));
        Common.actionBar(bob, "<green>%player_name%");
        Common.sendTitle(bob, "<gold>%player_name%", "<gray>Welcome %player_name%", 1, 2, 3);

        verify(alice).sendMessage(Common.component("Hello Alice"));
        verify(alice).sendActionBar(Common.component("Alice"));
        verify(bob).sendActionBar(Common.component("<green>Bob"));
        ArgumentCaptor<Title> title = ArgumentCaptor.forClass(Title.class);
        verify(bob).showTitle(title.capture());
        assertEquals(Common.component("<gold>Bob"), title.getValue().title());
        assertEquals(Common.component("<gray>Welcome Bob"), title.getValue().subtitle());
        assertEquals(java.time.Duration.ofMillis(50), title.getValue().times().fadeIn());
    }

    @Test
    void consoleSkipsPapiAndMissingOrFailingPapiLeavesMessagesReadable() {
        Common.sendMessage(console, "actionbar;Hello %player_name%");
        papi.verifyNoInteractions();
        verify(console).sendMessage(Common.component("Hello %player_name%"));

        when(plugins.isPluginEnabled("PlaceholderAPI")).thenReturn(false);
        Common.sendMessage(alice, "Hello %player_name%");
        papi.verifyNoInteractions();
        verify(alice).sendMessage(Common.component("Hello %player_name%"));

        when(plugins.isPluginEnabled("PlaceholderAPI")).thenReturn(true);
        papi.when(() -> PlaceholderAPI.setPlaceholders(bob, "Hello %player_name%"))
                .thenThrow(new IllegalStateException("Expansion failed"));
        Common.sendMessage(bob, "Hello %player_name%");
        verify(bob).sendMessage(Common.component("Hello %player_name%"));
    }

    @Test
    void broadcastsPersonalizeEachPlayerAndKeepPermissionsAndConsoleDelivery() {
        when(plugins.getPermissionSubscriptions(Server.BROADCAST_CHANNEL_USERS))
                .thenReturn(new HashSet<>(Set.of(alice, bob, console)));
        when(alice.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(true);
        when(bob.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(true);
        when(console.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(true);
        Config config = mock(Config.class, CALLS_REAL_METHODS);
        YamlConfiguration yaml = new YamlConfiguration();
        doReturn(yaml).when(config).getConfig();
        yaml.set("broadcast", List.of("<green>Hello {name}"));

        config.broadcast("broadcast", new Placeholder().add("{name}", "%player_name%"));

        verify(alice).sendMessage(Common.component("<green>Hello Alice"));
        verify(bob).sendMessage(Common.component("<green>Hello Bob"));
        verify(console).sendMessage(Common.component("<green>Hello %player_name%"));
        verify(plugins).callEvent(any(BroadcastMessageEvent.class));
    }

    @Test
    void broadcastsRespectCancellationAndPermissionDenials() {
        when(plugins.getPermissionSubscriptions(Server.BROADCAST_CHANNEL_USERS))
                .thenReturn(new HashSet<>(Set.of(alice, bob)));
        when(alice.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(true);
        doAnswer(invocation -> {
            BroadcastMessageEvent event = invocation.getArgument(0);
            assertEquals(Set.of(alice), event.getRecipients());
            event.setCancelled(true);
            return null;
        }).when(plugins).callEvent(any(BroadcastMessageEvent.class));

        Common.broadcast("Hello %player_name%");

        verify(alice, never()).sendMessage(any(Component.class));
        verify(bob, never()).sendMessage(any(Component.class));
        papi.verifyNoInteractions();
    }

    @Test
    void broadcastEventChangesStillApplyAndResolvePlayerPlaceholders() {
        when(plugins.getPermissionSubscriptions(Server.BROADCAST_CHANNEL_USERS))
                .thenReturn(new HashSet<>(Set.of(alice, bob)));
        when(alice.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(true);
        when(bob.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(true);
        doAnswer(invocation -> {
            BroadcastMessageEvent event = invocation.getArgument(0);
            event.getRecipients().remove(bob);
            event.message(Component.text("Changed %player_name%"));
            return null;
        }).when(plugins).callEvent(any(BroadcastMessageEvent.class));

        Common.broadcast("Hello %player_name%");

        verify(alice).sendMessage(Component.text("Changed ").append(Component.text("Alice")));
        verify(bob, never()).sendMessage(any(Component.class));
    }

    @Test
    void commandFeedbackAlsoUsesThePlayerContext() {
        RoutedCommand command = new RoutedCommand("test") {};

        command.execute(alice, "test", new String[]{"%player_name%"});

        verify(alice).sendMessage("§cUnknown subcommand: §fAlice");
    }
}
