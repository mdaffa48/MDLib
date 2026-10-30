package com.muhammaddaffa.mdlib;

import com.muhammaddaffa.mdlib.commands.CommandRegistry;
import com.muhammaddaffa.mdlib.commands.commands.BaseCommand;
import com.muhammaddaffa.mdlib.core.LogSink;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MDLibLifecycleTest {

    @TempDir
    Path directory;
    private final Object plugin = new Object();
    private ProxyServer server;
    private CommandManager commands;
    private CommandMeta meta;
    private ScheduledTask task;
    private Scheduler scheduler;
    private Scheduler.TaskBuilder taskBuilder;
    private org.slf4j.Logger logger;

    @BeforeEach
    void setUp() {
        server = mock(ProxyServer.class);
        commands = mock(CommandManager.class);
        meta = mock(CommandMeta.class);
        task = mock(ScheduledTask.class);
        logger = mock(org.slf4j.Logger.class);
        CommandMeta.Builder metaBuilder = mock(CommandMeta.Builder.class, RETURNS_SELF);
        scheduler = mock(Scheduler.class);
        taskBuilder = mock(Scheduler.TaskBuilder.class, RETURNS_SELF);
        when(server.getCommandManager()).thenReturn(commands);
        when(server.getScheduler()).thenReturn(scheduler);
        when(commands.metaBuilder(anyString())).thenReturn(metaBuilder);
        when(metaBuilder.build()).thenReturn(meta);
        when(scheduler.buildTask(eq(plugin), any(Runnable.class))).thenReturn(taskBuilder);
        when(taskBuilder.schedule()).thenReturn(task);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    @Test
    void onEnableStoresPlatformObjects() {
        MDLib.onEnable(plugin, server, logger, directory);

        assertSame(plugin, MDLib.instance());
        assertSame(server, MDLib.server());
        assertSame(directory, MDLib.dataDirectory());
        assertNotNull(MDLib.getCommandRegistry());
        assertNotNull(MDLib.getTaskProvider());
    }

    @Test
    void samePluginTwiceIsNoOpAndOtherPluginThrows() {
        MDLib.onEnable(plugin, server, logger, directory);
        CommandRegistry registry = MDLib.getCommandRegistry();

        MDLib.onEnable(plugin, server, logger, directory);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> MDLib.onEnable(new Object(), server, logger, directory));

        assertSame(registry, MDLib.getCommandRegistry());
        assertSame(plugin, MDLib.instance());
        assertEquals("MDLib is already initialized for another plugin.", error.getMessage());
    }

    @Test
    void logSinkMapsJulLevelsToSlf4j() {
        MDLib.onEnable(plugin, server, logger, directory);

        Logger.severe("severe");
        Logger.warning("warning");
        Logger.info("info");
        LogSink.log(Level.CONFIG, "config");
        LogSink.log(Level.FINE, "fine");
        LogSink.log(Level.FINER, "finer");
        Logger.finest("finest");

        verify(logger).error("severe");
        verify(logger).warn("warning");
        verify(logger).info("info");
        verify(logger).debug("config");
        verify(logger).debug("fine");
        verify(logger).trace("finer");
        verify(logger).trace("finest");
    }

    @Test
    void shutdownCleansUpAndUninstallsLogSink() {
        MDLib.onEnable(plugin, server, logger, directory);
        new BaseCommand("test") {
            @Override
            public void execute(CommandSource sender, String label, String[] args) {
            }
        }.register();
        Executor.asyncLater(20, () -> {});

        MDLib.shutdown();
        Logger.info("after shutdown");

        verify(commands).unregister(meta);
        verify(task).cancel();
        verify(logger, never()).info("after shutdown");
        assertNull(MDLib.instance());
        assertNull(MDLib.server());
        assertNull(MDLib.dataDirectory());
        assertNull(MDLib.getCommandRegistry());
        assertNull(MDLib.getTaskProvider());
    }

    @Test
    void shutdownCancelsMdlibTasksButNotThePluginsOwn() {
        MDLib.onEnable(plugin, server, logger, directory);
        ScheduledTask pluginOwn = mock(ScheduledTask.class);
        when(scheduler.tasksByPlugin(plugin)).thenReturn(List.of(task, pluginOwn));
        Executor.async(() -> {});

        MDLib.shutdown();

        verify(task).cancel();
        verify(pluginOwn, never()).cancel();
        verify(scheduler, never()).tasksByPlugin(any());
    }

    @Test
    void executorPassesDelayAndPeriodInTicks() {
        MDLib.onEnable(plugin, server, logger, directory);

        Executor.asyncLater(3, () -> {});
        verify(taskBuilder).delay(150L, TimeUnit.MILLISECONDS);
        Executor.asyncTimer(2, 20, () -> {});

        verify(taskBuilder).delay(100L, TimeUnit.MILLISECONDS);
        verify(taskBuilder).repeat(1000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void failedEnableShutsDownAndRethrows() {
        when(server.getCommandManager()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> MDLib.onEnable(plugin, server, logger, directory));
        Logger.info("after failure");

        assertNull(MDLib.instance());
        assertNull(MDLib.getTaskProvider());
        verify(logger, never()).info("after failure");
    }

}
