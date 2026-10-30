package com.muhammaddaffa.mdlib.updatechecker;

import com.muhammaddaffa.mdlib.MDLib;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.util.ProxyVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UpdateCheckerTest {

    @TempDir
    Path directory;
    private final Object plugin = new Object();
    private EventManager events;
    private ConsoleCommandSource console;
    private org.slf4j.Logger logger;

    @BeforeEach
    void setUp() {
        ProxyServer server = mock(ProxyServer.class);
        PluginManager plugins = mock(PluginManager.class);
        PluginContainer container = mock(PluginContainer.class);
        PluginDescription description = mock(PluginDescription.class);
        Scheduler scheduler = mock(Scheduler.class);
        events = mock(EventManager.class);
        console = mock(ConsoleCommandSource.class);
        logger = mock(org.slf4j.Logger.class);
        when(server.getCommandManager()).thenReturn(mock(CommandManager.class));
        when(server.getPluginManager()).thenReturn(plugins);
        when(server.getEventManager()).thenReturn(events);
        when(server.getConsoleCommandSource()).thenReturn(console);
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getVersion()).thenReturn(new ProxyVersion("Velocity", "Velocity Contributors", "3.4.0"));
        when(plugins.ensurePluginContainer(plugin)).thenReturn(container);
        when(container.getDescription()).thenReturn(description);
        when(description.getId()).thenReturn("updatetest");
        when(description.getName()).thenReturn(Optional.of("UpdateTest"));
        when(description.getVersion()).thenReturn(Optional.of(" 1.0 "));
        // Runs every scheduled task right away so checks finish before the assertions
        when(scheduler.buildTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            Scheduler.TaskBuilder builder = mock(Scheduler.TaskBuilder.class, RETURNS_SELF);
            when(builder.schedule()).thenAnswer(ignored -> {
                runnable.run();
                return mock(ScheduledTask.class);
            });
            return builder;
        });
        MDLib.onEnable(plugin, server, logger, directory);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    private UpdateCheckEvent firedEvent() {
        ArgumentCaptor<UpdateCheckEvent> event = ArgumentCaptor.forClass(UpdateCheckEvent.class);
        verify(events).fire(event.capture());
        return event.getValue();
    }

    @SuppressWarnings("unchecked")
    private <E> EventHandler<E> handler(Class<E> type) {
        ArgumentCaptor<EventHandler<E>> handler = ArgumentCaptor.forClass(EventHandler.class);
        verify(events).register(eq(plugin), eq(type), handler.capture());
        return handler.getValue();
    }

    private static Component legacy(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

    @Test
    void newerSupplierVersionCallsOnSuccessAndFiresEvent() {
        List<String> successes = new ArrayList<>();
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0")
                .onSuccess((requesters, latest) -> successes.add(requesters.length + ":" + latest));

        checker.checkNow();

        assertEquals(List.of("1:2.0"), successes);
        UpdateCheckEvent event = firedEvent();
        assertEquals(UpdateCheckSuccess.SUCCESS, event.getSuccess());
        assertEquals(UpdateCheckResult.NEW_VERSION_AVAILABLE, event.getResult());
        assertEquals("2.0", event.getLatestVersion());
        assertEquals("1.0", event.getUsedVersion());
        assertArrayEquals(new CommandSource[]{console}, event.getRequesters());
    }

    @Test
    void shutdownDuringCheckDropsTheEvent() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0")
                .onSuccess((requesters, latest) -> MDLib.shutdown());

        assertDoesNotThrow(() -> checker.checkNow());

        assertNull(MDLib.server());
        verify(events, never()).fire(any(UpdateCheckEvent.class));
    }

    @Test
    void failingSupplierCallsOnFail() {
        IOException failure = new IOException("offline");
        List<Exception> failures = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        UpdateChecker checker = new UpdateChecker(plugin, () -> {
            throw failure;
        }).onFail((requesters, ex) -> failures.add(ex)).onSuccess((requesters, latest) -> successes.add(latest));

        checker.checkNow();

        assertEquals(List.of(failure), failures);
        assertTrue(successes.isEmpty());
        UpdateCheckEvent event = firedEvent();
        assertEquals(UpdateCheckSuccess.FAIL, event.getSuccess());
        assertEquals(UpdateCheckResult.UNKNOWN, event.getResult());
    }

    @Test
    void listenerIsRegisteredOnceAndReportsToRequesters() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0");
        new UpdateChecker(plugin, () -> "3.0");
        Player player = mock(Player.class);

        checker.checkNow(player, console);
        handler(UpdateCheckEvent.class).execute(firedEvent());

        verify(player).sendMessage(legacy("§7There is a new version of §6UpdateTest§7 available."));
        verify(player).sendMessage(legacy("§8Latest version: §a2.0§8 | Your version: §c1.0"));
        verify(logger).warn("* There is a new version of UpdateTest available!");
        verify(logger).warn("* Latest version: 2.0");
    }

    @Test
    void joinNotifiesOnlyPlayersWithPermission() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0").setNotifyByPermissionOnJoin("updates.notify");
        Player allowed = mock(Player.class);
        Player denied = mock(Player.class);
        when(allowed.hasPermission("updates.notify")).thenReturn(true);
        checker.checkNow();
        EventHandler<PostLoginEvent> join = handler(PostLoginEvent.class);

        join.execute(new PostLoginEvent(allowed));
        join.execute(new PostLoginEvent(denied));

        verify(allowed).sendMessage(legacy("§7There is a new version of §6UpdateTest§7 available."));
        verify(denied, never()).sendMessage(any(Component.class));
    }

    @Test
    void joinNotifiesNobodyWithoutNotifyPermission() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0");
        Player player = mock(Player.class);
        when(player.hasPermission(any())).thenReturn(true);
        checker.checkNow();
        assertEquals(UpdateCheckResult.NEW_VERSION_AVAILABLE, checker.getLastCheckResult());

        handler(PostLoginEvent.class).execute(new PostLoginEvent(player));

        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void joinStaysQuietWhenNoNewVersionIsKnown() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> {
            throw new IOException("offline");
        }).onFail((requesters, ex) -> {}).setNotifyByPermissionOnJoin("updates.notify");
        Player allowed = mock(Player.class);
        when(allowed.hasPermission("updates.notify")).thenReturn(true);
        checker.checkNow();

        handler(PostLoginEvent.class).execute(new PostLoginEvent(allowed));

        verify(allowed, never()).sendMessage(any(Component.class));
    }

    @Test
    void shutdownOnlyClosesOwnersChecker() {
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2.0");
        EventHandler<PostLoginEvent> join = handler(PostLoginEvent.class);
        EventHandler<UpdateCheckEvent> update = handler(UpdateCheckEvent.class);

        UpdateChecker.shutdown(new Object());
        assertSame(checker, UpdateChecker.getInstance());
        verify(events, never()).unregister(any(), any());
        assertDoesNotThrow(() -> checker.checkNow());
        assertEquals(UpdateCheckSuccess.SUCCESS, firedEvent().getSuccess());

        UpdateChecker.shutdown(plugin);
        assertNull(UpdateChecker.getInstance());
        verify(events).unregister(plugin, join);
        verify(events).unregister(plugin, update);
        verify(events, never()).unregisterListeners(any());
        assertThrows(IllegalStateException.class, checker::checkNow);
    }

    @Test
    void defaultUserAgentNamesPluginAndProxy() {
        new UpdateChecker(plugin, () -> "2.0");

        assertEquals("JEFF-Media-GbR-SpigotUpdateChecker/3.0.4 (UpdateTest/ 1.0 , ServerVersion/Velocity 3.4.0)",
                UserAgentBuilder.getDefaultUserAgent().build());
    }

}
