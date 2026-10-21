package com.muhammaddaffa.mdlib.task;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.task.provider.FoliaProvider;
import com.muhammaddaffa.mdlib.utils.FoliaExecutor;
import io.papermc.paper.threadedregions.scheduler.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FoliaSchedulingTest {

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<MDLib> mdlib;
    private GlobalRegionScheduler global;
    private RegionScheduler region;
    private AsyncScheduler async;
    private ScheduledTask task;
    private JavaPlugin plugin;
    private final FoliaProvider provider = new FoliaProvider();

    @BeforeEach
    void setUp() {
        global = mock(GlobalRegionScheduler.class);
        region = mock(RegionScheduler.class);
        async = mock(AsyncScheduler.class);
        task = mock(ScheduledTask.class);
        plugin = mock(JavaPlugin.class);
        bukkit = mockStatic(Bukkit.class);
        mdlib = mockStatic(MDLib.class);
        mdlib.when(MDLib::instance).thenReturn(plugin);
        bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(global);
        bukkit.when(Bukkit::getRegionScheduler).thenReturn(region);
        bukkit.when(Bukkit::getAsyncScheduler).thenReturn(async);
    }

    @AfterEach
    void tearDown() {
        ExecutorManager.shutdown();
        mdlib.close();
        bukkit.close();
    }

    @Test
    void globalAndRegionDelaysRemainInTicks() {
        World world = mock(World.class);
        Runnable runnable = mock(Runnable.class);
        when(global.runDelayed(eq(plugin), any(), eq(20L))).thenReturn(task);
        when(global.runAtFixedRate(eq(plugin), any(), eq(20L), eq(40L))).thenReturn(task);
        when(region.runDelayed(eq(plugin), eq(world), eq(1), eq(2), any(), eq(20L))).thenReturn(task);
        when(region.runAtFixedRate(eq(plugin), eq(world), eq(1), eq(2), any(), eq(20L), eq(40L))).thenReturn(task);

        assertSame(task, provider.syncLater(20, runnable).getRaw());
        assertSame(task, provider.syncTimer(20, 40, runnable).getRaw());
        assertSame(task, provider.regionLater(world, 1, 2, 20, runnable).getRaw());
        assertSame(task, provider.regionTimer(world, 1, 2, 20, 40, runnable).getRaw());
        provider.syncLater(0, runnable);
        verify(global).runDelayed(eq(plugin), any(), eq(1L));
    }

    @Test
    void onlyAsyncSchedulerConvertsTicksToMilliseconds() {
        Runnable runnable = mock(Runnable.class);
        provider.asyncLater(20, runnable);
        provider.asyncTimer(20, 40, runnable);

        verify(async).runDelayed(eq(plugin), any(), eq(1000L), eq(TimeUnit.MILLISECONDS));
        verify(async).runAtFixedRate(eq(plugin), any(), eq(1000L), eq(2000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void playerAndEntityCallbacksRunOnTheirEntityScheduler() {
        Player player = mock(Player.class);
        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(player.getScheduler()).thenReturn(scheduler);
        when(scheduler.runAtFixedRate(eq(plugin), any(), isNull(), eq(1L), eq(40L))).thenReturn(task);
        Runnable runnable = mock(Runnable.class);

        HandleTask handle = provider.playerTimer(player, 0, 40, runnable);

        ArgumentCaptor<Consumer<ScheduledTask>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(scheduler).runAtFixedRate(eq(plugin), callback.capture(), isNull(), eq(1L), eq(40L));
        callback.getValue().accept(task);
        verify(runnable).run();
        assertSame(task, handle.getRaw());
        provider.entity(player, runnable);
        provider.entityLater(player, 20, runnable);
        verify(scheduler).run(eq(plugin), any(), isNull());
        verify(scheduler).runDelayed(eq(plugin), any(), isNull(), eq(20L));
        verifyNoInteractions(global);
    }

    @Test
    void retiredEntitiesReturnACancelledHandleWithoutGlobalFallback() {
        Player player = mock(Player.class);
        when(player.getScheduler()).thenReturn(mock(EntityScheduler.class));
        Runnable runnable = mock(Runnable.class);

        HandleTask handle = provider.playerTimer(player, 20, 40, runnable);

        assertTrue(handle.isCancelled());
        assertNull(handle.getRaw());
        assertDoesNotThrow(handle::cancel);
        verifyNoInteractions(global, runnable);
    }

    @Test
    void shutdownCancelsOnlyTrackedBukkitFoliaAndDirectExecutorTasks() {
        BukkitTask owned = mock(BukkitTask.class);
        BukkitTask unrelated = mock(BukkitTask.class);
        new HandleTask(owned);
        when(global.run(eq(plugin), any())).thenReturn(task);
        assertSame(task, FoliaExecutor.runTask(ignored -> {}));

        ExecutorManager.shutdown();
        ExecutorManager.shutdown();

        verify(owned).cancel();
        verify(task).cancel();
        verifyNoInteractions(unrelated);
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyConsumerOverloadsKeepTickTiming() {
        provider.entityLater(ignored -> {}, 20, () -> {});
        provider.entityTimer(ignored -> {}, 20, 40, () -> {});

        verify(global).runDelayed(eq(plugin), any(), eq(20L));
        verify(global).runAtFixedRate(eq(plugin), any(), eq(20L), eq(40L));
    }
}
