package com.muhammaddaffa.mdlib;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.updatechecker.UpdateChecker;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.worldguards.WgEntity;
import com.muhammaddaffa.mdlib.fastinv.FastInvManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MDLibLifecycleTest {

    @TempDir
    Path directory;
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<FastInvManager> inventories;
    private JavaPlugin plugin;
    private PluginManager plugins;
    private CommandMap commands;
    private final HandlerList handlers = new HandlerList();
    private final Map<String, Command> knownCommands = new HashMap<>();

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        plugins = mock(PluginManager.class);
        commands = mock(CommandMap.class);
        Server server = mock(Server.class);
        when(plugin.getName()).thenReturn("MDLibTest");
        when(plugin.getLogger()).thenReturn(mock(java.util.logging.Logger.class));
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("MDLibTest", "1", "example.Main"));
        when(commands.getKnownCommands()).thenReturn(knownCommands);
        when(server.getName()).thenReturn("Paper");
        bukkit = mockStatic(Bukkit.class);
        inventories = mockStatic(FastInvManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        bukkit.when(Bukkit::getCommandMap).thenReturn(commands);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        bukkit.when(Bukkit::getVersion).thenReturn("Paper test");
        doAnswer(invocation -> {
            Listener listener = invocation.getArgument(0);
            handlers.register(new RegisteredListener(listener, (ignored, event) -> {},
                    EventPriority.NORMAL, plugin, false));
            return null;
        }).when(plugins).registerEvents(any(Listener.class), eq(plugin));
        inventories.when(() -> FastInvManager.register(plugin)).thenAnswer(invocation -> {
            plugins.registerEvents(mock(Listener.class), plugin);
            return null;
        });
        when(commands.register(eq("mdlibtest"), any(Command.class))).thenAnswer(invocation -> {
            Command command = invocation.getArgument(1);
            knownCommands.put(command.getName(), command);
            return true;
        });
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
        HandlerList.unregisterAll(plugin);
        WgEntity.getPlayerCache().clear();
        inventories.close();
        bukkit.close();
    }

    @Test
    void shutdownClearsOwnedStateAndLeavesHostRegistrationsAndTasksAlone() throws Exception {
        Listener hostListener = mock(Listener.class);
        handlers.register(new RegisteredListener(hostListener, (ignored, event) -> {},
                EventPriority.NORMAL, plugin, false));
        Command hostCommand = mock(Command.class);
        knownCommands.put("host", hostCommand);
        when(plugins.isPluginEnabled("WorldGuard")).thenReturn(true);
        MDLib.registerWorldGuard();
        MDLib.onEnable(plugin);
        MDLib.onEnable(plugin);
        MDLib.getCommandRegistry().register(new RoutedCommand("owned") {});
        Files.writeString(directory.resolve("config.yml"), "value: test\n");
        new Config("config.yml", null, true);
        Common.registerTagResolver("lifecycle", Placeholder.component("lifecycle", Component.text("Registered")));
        WgEntity.getPlayerCache().put(UUID.randomUUID(), mock(WgEntity.class));
        BukkitTask ownedTask = mock(BukkitTask.class);
        BukkitTask hostTask = mock(BukkitTask.class);
        new HandleTask(ownedTask);
        assertEquals(Component.text("Registered"), Common.component("<lifecycle>"));

        MDLib.shutdown();
        MDLib.shutdown();

        assertNull(MDLib.instance());
        assertNull(MDLib.getCommandRegistry());
        assertEquals(Map.of("host", hostCommand), knownCommands);
        assertEquals(1, HandlerList.getRegisteredListeners(plugin).size());
        assertSame(hostListener, HandlerList.getRegisteredListeners(plugin).getFirst().getListener());
        assertTrue(WgEntity.getPlayerCache().isEmpty());
        assertEquals(Component.text("<lifecycle>"), Common.component("<lifecycle>"));
        var configMap = Config.class.getDeclaredField("configMap");
        configMap.setAccessible(true);
        assertTrue(((Map<?, ?>) configMap.get(null)).isEmpty());
        verify(ownedTask).cancel();
        verifyNoInteractions(hostTask);
        inventories.verify(() -> FastInvManager.register(plugin), times(1));
        MDLib.onEnable(plugin);
        assertSame(plugin, MDLib.instance());
    }

    @Test
    void failedInitializationReleasesPartialRegistrations() {
        when(plugins.isPluginEnabled("WorldGuard")).thenReturn(true);
        MDLib.registerWorldGuard();
        inventories.reset();
        inventories.when(() -> FastInvManager.register(plugin)).thenThrow(new IllegalStateException("Registration failed"));

        assertThrows(IllegalStateException.class, () -> MDLib.onEnable(plugin));

        assertNull(MDLib.instance());
        assertNull(MDLib.getCommandRegistry());
        assertTrue(HandlerList.getRegisteredListeners(plugin).isEmpty());
    }

    @Test
    void capabilityDetectionFindsPaperAndEntityRemovalAndKeepsFoliaNameFallback() {
        assertTrue(MDLib.isPaper());
        assertTrue(MDLib.is1_20_4OrNewer());
        assertFalse(MDLib.isFolia());
        when(Bukkit.getServer().getName()).thenReturn("Folia");
        assertTrue(MDLib.isFolia());
    }

    @Test
    void updateCheckerShutdownReleasesItsListenerAndAllowsANewInstance() {
        MDLib.onEnable(plugin);
        UpdateChecker checker = new UpdateChecker(plugin, () -> "2");
        JavaPlugin other = mock(JavaPlugin.class);
        UpdateChecker.shutdown(other);
        assertSame(checker, UpdateChecker.getInstance());

        MDLib.shutdown();

        assertNull(UpdateChecker.getInstance());
        assertThrows(IllegalStateException.class, () -> checker.checkNow());
        assertTrue(HandlerList.getRegisteredListeners(plugin).isEmpty());
        MDLib.onEnable(plugin);
        UpdateChecker replacement = new UpdateChecker(plugin, () -> "3");
        assertSame(replacement, UpdateChecker.getInstance());
        assertEquals(2, HandlerList.getRegisteredListeners(plugin).size());
    }
}
