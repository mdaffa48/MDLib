package com.muhammaddaffa.mdlib.updatechecker;

import com.muhammaddaffa.mdlib.MDLib;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.event.EventHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateCheckerTest {

    private Plugin plugin;
    private ProxyServer proxy;
    private PluginManager plugins;
    private CommandSender console;
    private Logger logger;
    private final Events events = new Events();

    public static final class Events implements Listener {

        final List<UpdateCheckEvent> seen = new ArrayList<>();

        @EventHandler
        public void onUpdateCheck(UpdateCheckEvent event) {
            seen.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        proxy = mock(ProxyServer.class);
        when(proxy.getLogger()).thenReturn(Logger.getLogger("UpdateCheckerTest"));
        plugins = new PluginManager(proxy);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(scheduler.runAsync(any(), any())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return mock(ScheduledTask.class);
        });
        console = mock(CommandSender.class);
        logger = mock(Logger.class);
        when(proxy.getPluginManager()).thenReturn(plugins);
        when(proxy.getScheduler()).thenReturn(scheduler);
        when(proxy.getConsole()).thenReturn(console);
        PluginDescription description = new PluginDescription();
        description.setName("UpdateTest");
        description.setVersion(" 1.0 ");
        plugin = mock(Plugin.class);
        when(plugin.getProxy()).thenReturn(proxy);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDescription()).thenReturn(description);
        MDLib.onEnable(plugin);
        plugins.registerListener(plugin, events);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    private static List<BaseComponent> chat(CommandSender sender) {
        ArgumentCaptor<BaseComponent> sent = ArgumentCaptor.forClass(BaseComponent.class);
        verify(sender, atLeast(0)).sendMessage(sent.capture());
        return sent.getAllValues();
    }

    @Test
    void newerSuppliedVersionReachesOnSuccessTheEventAndTheConsoleReport() {
        List<String> found = new ArrayList<>();
        UpdateChecker checker = new UpdateChecker(plugin, () -> "1.1")
                .setUserAgent("test")
                .setDownloadLink("https://example.org/download")
                .onSuccess((requesters, latest) -> found.add(latest));

        assertEquals("1.0", checker.getUsedVersion());
        assertEquals(UpdateCheckResult.UNKNOWN, checker.getLastCheckResult());

        checker.checkNow();

        assertEquals(List.of("1.1"), found);
        assertEquals(UpdateCheckResult.NEW_VERSION_AVAILABLE, checker.getLastCheckResult());
        assertEquals(1, events.seen.size());
        UpdateCheckEvent event = events.seen.get(0);
        assertEquals(UpdateCheckSuccess.SUCCESS, event.getSuccess());
        assertEquals(UpdateCheckResult.NEW_VERSION_AVAILABLE, event.getResult());
        assertEquals("1.1", event.getLatestVersion());
        assertEquals("1.0", event.getUsedVersion());
        assertArrayEquals(new CommandSender[]{console}, event.getRequesters());
        verify(logger).log(Level.WARNING, "* There is a new version of UpdateTest available!");
        verify(logger).log(Level.WARNING, "*   https://example.org/download");
    }

    @Test
    void failedFetchGoesToOnFailAndFiresAFailEvent() {
        IOException failure = new IOException("offline");
        List<Exception> failures = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        ProxiedPlayer requester = mock(ProxiedPlayer.class);
        UpdateChecker checker = new UpdateChecker(plugin, () -> {
            throw failure;
        }).setUserAgent("test")
                .setNotifyByPermissionOnJoin("update.notify")
                .onFail((requesters, ex) -> failures.add(ex))
                .onSuccess((requesters, latest) -> successes.add(latest));

        checker.checkNow(requester);
        ProxiedPlayer admin = mock(ProxiedPlayer.class);
        when(admin.hasPermission("update.notify")).thenReturn(true);
        plugins.callEvent(new PostLoginEvent(admin, null, (result, error) -> {}));
        verify(admin, never()).sendMessage(any(BaseComponent.class));

        assertEquals(List.of(failure), failures);
        assertTrue(successes.isEmpty());
        assertNull(checker.getLatestVersion());
        assertEquals(UpdateCheckSuccess.FAIL, events.seen.get(0).getSuccess());
        assertEquals(UpdateCheckResult.UNKNOWN, events.seen.get(0).getResult());
        assertEquals(List.of("UpdateTest could not check for updates."),
                chat(requester).stream().map(component -> component.toPlainText()).toList());
    }

    @Test
    void playersWithTheNotifyPermissionAreToldOnLogin() {
        new UpdateChecker(plugin, () -> "2.0")
                .setUserAgent("test")
                .setNotifyRequesters(false)
                .setDownloadLink("https://example.org/download")
                .setNotifyByPermissionOnJoin("update.notify")
                .checkNow();
        ProxiedPlayer admin = mock(ProxiedPlayer.class);
        ProxiedPlayer guest = mock(ProxiedPlayer.class);
        when(admin.hasPermission("update.notify")).thenReturn(true);

        plugins.callEvent(new PostLoginEvent(admin, null, (result, error) -> {}));
        plugins.callEvent(new PostLoginEvent(guest, null, (result, error) -> {}));

        List<BaseComponent> sent = chat(admin);
        assertEquals("There is a new version of UpdateTest available.", sent.get(0).toPlainText());
        assertEquals("Download", sent.get(1).toPlainText());
        ClickEvent click = sent.get(1).getExtra().get(0).getClickEvent();
        assertEquals(ClickEvent.Action.OPEN_URL, click.getAction());
        assertEquals("https://example.org/download", click.getValue());
        assertEquals("Latest version: 2.0 | Your version: 1.0", sent.get(2).toPlainText());
        verify(guest, never()).sendMessage(any(BaseComponent.class));
        verify(logger, never()).log(eq(Level.WARNING), anyString());
    }

    @Test
    void nobodyIsToldOnLoginWithoutANotifyPermission() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0")
                .setUserAgent("test")
                .setNotifyRequesters(false)
                .setDownloadLink("https://example.org/download")
                .checkNow();
        assertEquals(UpdateCheckResult.NEW_VERSION_AVAILABLE, checker.getLastCheckResult());
        ProxiedPlayer player = mock(ProxiedPlayer.class);
        when(player.hasPermission(any())).thenReturn(true);

        plugins.callEvent(new PostLoginEvent(player, null, (result, error) -> {}));

        verify(player, never()).sendMessage(any(BaseComponent.class));
    }

    @Test
    void shutdownOfAnotherOwnerLeavesTheCheckerUsable() {
        List<String> found = new ArrayList<>();
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0").setUserAgent("test")
                .setNotifyRequesters(false)
                .setNotifyByPermissionOnJoin("update.notify")
                .onSuccess((requesters, latest) -> found.add(latest));

        UpdateChecker.shutdown(mock(Plugin.class));

        assertSame(checker, UpdateChecker.getInstance());
        assertDoesNotThrow(() -> checker.checkNow());
        assertEquals(List.of("2.0"), found);
        assertEquals(1, events.seen.size());
        ProxiedPlayer admin = mock(ProxiedPlayer.class);
        when(admin.hasPermission("update.notify")).thenReturn(true);
        plugins.callEvent(new PostLoginEvent(admin, null, (result, error) -> {}));
        assertFalse(chat(admin).isEmpty());
    }

    @Test
    void shutdownOnlyStopsTheOwnersCheckerAndItsListener() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0").setUserAgent("test")
                .setNotifyByPermissionOnJoin("update.notify").checkNow();

        UpdateChecker.shutdown(mock(Plugin.class));
        assertSame(checker, UpdateChecker.getInstance());

        UpdateChecker.shutdown(plugin);
        ProxiedPlayer admin = mock(ProxiedPlayer.class);
        when(admin.hasPermission("update.notify")).thenReturn(true);
        plugins.callEvent(new PostLoginEvent(admin, null, (result, error) -> {}));

        assertNull(UpdateChecker.getInstance());
        assertThrows(IllegalStateException.class, checker::checkNow);
        verify(admin, never()).sendMessage(any(BaseComponent.class));
    }

    @Test
    void defaultUserAgentNamesThePluginAndTheProxy() {
        when(proxy.getName()).thenReturn("BungeeCord");
        when(proxy.getVersion()).thenReturn("1.21");
        new UpdateChecker(plugin, UpdateCheckSource.SPIGOT, "1");

        assertEquals("JEFF-Media-GbR-SpigotUpdateChecker/3.0.4 (UpdateTest/ 1.0 , ServerVersion/BungeeCord 1.21, Paid/false)",
                UserAgentBuilder.getDefaultUserAgent().addUsingPaidVersion().build());
        assertTrue(UpdateChecker.isOtherVersionNewer("1.0", "1.1"));
    }

}
