package com.muhammaddaffa.mdlib;

import com.muhammaddaffa.mdlib.commands.CommandRegistry;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.updatechecker.UpdateChecker;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MDLibLifecycleTest {

    @TempDir
    Path directory;
    private Plugin plugin;
    private PluginManager plugins;
    private TaskScheduler scheduler;
    private java.util.logging.Logger logger;

    @BeforeEach
    void setUp() {
        ProxyServer proxy = mock(ProxyServer.class);
        when(proxy.getLogger()).thenReturn(java.util.logging.Logger.getLogger("MDLibLifecycleTest"));
        plugins = new PluginManager(proxy);
        scheduler = mock(TaskScheduler.class);
        logger = mock(java.util.logging.Logger.class);
        when(proxy.getPluginManager()).thenReturn(plugins);
        when(proxy.getScheduler()).thenReturn(scheduler);
        plugin = mock(Plugin.class);
        PluginDescription description = new PluginDescription();
        description.setName("MDLibTest");
        description.setVersion("1");
        when(plugin.getProxy()).thenReturn(proxy);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getDescription()).thenReturn(description);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    @Test
    void samePluginIsANoOpAndAnotherPluginIsRejected() {
        MDLib.onEnable(plugin);
        CommandRegistry registry = MDLib.getCommandRegistry();

        MDLib.onEnable(plugin);

        assertSame(registry, MDLib.getCommandRegistry());
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> MDLib.onEnable(mock(Plugin.class)));
        assertEquals("MDLib is already initialized for another plugin.", failure.getMessage());
        assertSame(plugin, MDLib.instance());
        assertThrows(NullPointerException.class, () -> MDLib.onEnable(null));
    }

    @Test
    void loggerWritesToThePluginLoggerUntilShutdown() {
        MDLib.onEnable(plugin);
        Logger.info("enabled");

        MDLib.shutdown();
        Logger.info("disabled");

        verify(logger).log(Level.INFO, "enabled");
        verify(logger, never()).log(Level.INFO, "disabled");
    }

    @Test
    void failedEnableShutsDownAndRethrows() {
        doThrow(new IllegalStateException("no proxy")).when(plugin).getProxy();

        assertThrows(IllegalStateException.class, () -> MDLib.onEnable(plugin));

        assertNull(MDLib.instance());
        assertNull(MDLib.getTaskProvider());
        assertNull(MDLib.getCommandRegistry());
    }

    @Test
    void executorPassesTheDelayThenThePeriodInMilliseconds() {
        MDLib.onEnable(plugin);
        Runnable runnable = () -> {};

        Executor.asyncLater(3, runnable);
        Executor.asyncTimer(2, 4, runnable);

        verify(scheduler).schedule(plugin, runnable, 150L, TimeUnit.MILLISECONDS);
        verify(scheduler).schedule(plugin, runnable, 100L, 200L, TimeUnit.MILLISECONDS);
    }

    @Test
    void shutdownClearsOwnedStateAndLeavesHostCommandsAndTasksAlone() throws Exception {
        Command host = new Command("host") {
            @Override
            public void execute(CommandSender sender, String[] args) {
            }
        };
        plugins.registerCommand(plugin, host);
        ScheduledTask owned = mock(ScheduledTask.class);
        ScheduledTask hostTask = mock(ScheduledTask.class);
        when(scheduler.runAsync(eq(plugin), any())).thenReturn(owned);
        Files.writeString(directory.resolve("config.yml"), "value: first\n");

        MDLib.onEnable(plugin);
        new RoutedCommand("owned") {}.register();
        HandleTask task = Executor.async(() -> {});
        Config config = new Config("config.yml", null, true);
        Common.registerTagResolver("lifecycle", Placeholder.component("lifecycle", Component.text("Registered")));
        new UpdateChecker(plugin, () -> "1");
        assertEquals(Component.text("Registered"), Common.component("<lifecycle>"));

        MDLib.shutdown();
        MDLib.shutdown();

        assertNull(MDLib.instance());
        assertNull(MDLib.getCommandRegistry());
        assertNull(MDLib.getTaskProvider());
        assertFalse(plugins.isExecutableCommand("owned", mock(CommandSender.class)));
        assertTrue(plugins.isExecutableCommand("host", mock(CommandSender.class)));
        verify(owned).cancel();
        assertTrue(task.isCancelled());
        verifyNoInteractions(hostTask);
        verify(scheduler, never()).cancel(any(Plugin.class));
        assertNull(UpdateChecker.getInstance());
        assertNotEquals(Component.text("Registered"), Common.component("<lifecycle>"));
        Files.writeString(directory.resolve("config.yml"), "value: second\n");
        Config.reload();
        assertEquals("first", config.getString("value"));
        assertThrows(NullPointerException.class, () -> Executor.async(() -> {}));
    }

}
